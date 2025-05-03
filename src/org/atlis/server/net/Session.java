package org.atlis.server.net;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.atlis.common.model.Player;
import static org.atlis.common.model.UpdateFlag.WALKING;
import org.atlis.common.net.Packet;
import org.atlis.common.net.PacketBuilder;
import org.atlis.common.security.ISAAC;
import org.atlis.common.tsk.Task;
import org.atlis.common.util.Constants;
import org.atlis.server.Server;
import org.atlis.server.net.SessionPool.PlayerRegistry;
import org.atlis.server.net.sql.Database;

public class Session extends Task {

    public Player player;
    public final SocketChannel channel;
    public final SelectionKey key;
    public final ByteBuffer loginBuffer = ByteBuffer.allocate(128);
    public int loginReadStage = 0; // 0=header, 1=block
    public int loginBlockLength = -1;
    public boolean startedExchange;
    public PacketSender sender;

    public final ByteBuffer packetBuffer = ByteBuffer.allocate(1024); // incoming buffer
    public int expectedPacketSize = -1, currentOpcode;

    public Queue<PacketBuilder> outgoingPacketQueue;

    public SessionState state = SessionState.KEY_EXCHANGE;

    public ISAAC encryptor;
    public ISAAC decryptor;

    public Session(SocketChannel channel, SelectionKey key) {
        super(Constants.PARSING_INTERVAL);
        this.loginBuffer.clear();
        this.channel = channel;
        this.key = key;
        outgoingPacketQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void execute() {
        if (!isLoggedIn()) {
            return;
        }
        try {

            update();

            while (!outgoingPacketQueue.isEmpty()) {
                PacketBuilder pb = outgoingPacketQueue.poll();
                //System.out.println("Attempting to handle outgoing packet: " + pb.opcode);
                byte[] packetData = pb.toPacket();
                ByteBuffer buffer = ByteBuffer.wrap(packetData);

                channel.write(buffer);

                if (buffer.hasRemaining()) {
                    break;
                }
            }

            if (outgoingPacketQueue.isEmpty()) {
                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
            }

        } catch (IOException e) {
            e.printStackTrace();
            close();
        }
    }

    public void update() {
        if (player == null || player.region == null) {
            return;
        }

        // Example: WALKING
        if (player.hasUpdateFlag(WALKING)) {
            for (Player other : SessionPool.PlayerRegistry.all().values()) {
                if (other == player) {
                    continue;
                }
                if (other.region == null) {
                    continue;
                }
                if (player.getCurrentRegionId() != other.getCurrentRegionId()) {
                    continue;
                }

                Session otherSession = PlayerRegistry.getSessionByPlayer(other);
                if (otherSession == null) {
                    continue;
                }

                PacketBuilder builder = new PacketBuilder(6, encryptor); // PlayerUpdate opcode
                builder.addLong(player.getId());
                builder.addInt(player.getX());
                builder.addInt(player.getY());
                builder.addByte((byte) WALKING.getBit());
                otherSession.queuePacket(builder);
            }

        }
        player.clearUpdateFlags();
        // Future: handle other flags (APPEARANCE, CHAT, etc.)
    }

    public void read(SelectionKey key, ByteBuffer buffer) {
        try {
            if (getState() == SessionState.KEY_EXCHANGE || getState() == SessionState.PROTOCOL) {
                int bytesRead = channel.read(loginBuffer);
                if (bytesRead == -1) {
                    System.out.println("Client closed during login: " + channel.getRemoteAddress());
                    SessionPool.unregister(channel);
                    key.cancel();
                    return;
                }

                loginBuffer.flip();

                if (loginReadStage == 0 && loginBuffer.remaining() >= 1) {
                    byte hello = loginBuffer.get();
                    if (hello != 0x00) {
                        System.out.println("Unexpected hello byte: " + hello);
                        close();
                        return;
                    }
                    LoginProtocol.exchangeKeys(this);
                    loginReadStage = 1;
                    loginBuffer.clear();
                    return;
                }

                if (loginReadStage == 1 && loginBuffer.remaining() >= 3) {
                    byte opcode = loginBuffer.get();
                    int length = loginBuffer.getShort();
                    loginBlockLength = length;
                    loginReadStage = 2;
                }

                if (loginReadStage == 2 && loginBuffer.remaining() >= loginBlockLength) {
                    LoginProtocol.handle(this, loginBuffer);
                    setState(SessionState.LOGGED_IN);
                    //System.out.println("After login handle, remaining bytes: " + loginBuffer.remaining());
                    loginBuffer.clear();
                    return;
                }

                loginBuffer.compact();
                return;
            }

            if (getState() == SessionState.LOGGED_IN) {
                buffer.clear();
                int bytesRead = channel.read(buffer);
                //System.out.println("bytes read after login: " + bytesRead);
                if (bytesRead == 0) {
                    // No data available yet, return safely
                    return;
                }

                if (bytesRead == -1) {
                    // System.out.println("Disconnected after login: " + channel.getRemoteAddress());
                    if (getPlayer() != null) {
                        Database.savePlayer(getPlayer());
                    }
                    SessionPool.unregister(channel);
                    key.cancel();
                    return;
                }

                // Only continue if we actually received data
                buffer.flip();
                packetBuffer.put(buffer); // append new data
                packetBuffer.flip();

                while (getState() == SessionState.LOGGED_IN) {
                    if (expectedPacketSize == -1) {
                        if (packetBuffer.remaining() < 3) {
                            packetBuffer.compact();
                            return;
                        }

                        int encryptedOpcode = packetBuffer.get() & 0xFF;
                        currentOpcode = (byte) ((encryptedOpcode - decryptor.next()) & 0xFF);
                        expectedPacketSize = packetBuffer.getShort() & 0xFFFF;
                        packetBuffer.mark();
                    }

                    if (packetBuffer.remaining() < expectedPacketSize) {
                        packetBuffer.reset();
                        packetBuffer.compact();
                        return;
                    }

                    byte[] payload = new byte[expectedPacketSize];
                    packetBuffer.get(payload);
                    Packet packet = new Packet(payload, currentOpcode);
                    PacketManager.process(packet, this);

                    expectedPacketSize = -1;
                    packetBuffer.compact().flip();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Session dropped for " + player.getUsername());
            close();
        }
    }

    public void close() {
        try {
            key.cancel();
            channel.close();
            Database.savePlayer(player);
            Server.getTaskPool().remove(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void queuePacket(PacketBuilder pb) {
        outgoingPacketQueue.add(pb);
        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        key.selector().wakeup();
    }

    public void setCiphers(ISAAC encryptor, ISAAC decryptor) {
        this.encryptor = encryptor;
        this.decryptor = decryptor;
    }

    public ISAAC getEncryptor() {
        return encryptor;
    }

    public ISAAC getDecryptor() {
        return decryptor;
    }

    public ByteBuffer getLoginBuffer() {
        return loginBuffer;
    }

    public int getLoginReadStage() {
        return loginReadStage;
    }

    public void setLoginReadStage(int stage) {
        this.loginReadStage = stage;
    }

    public int getLoginBlockLength() {
        return loginBlockLength;
    }

    public void setLoginBlockLength(int length) {
        this.loginBlockLength = length;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public SelectionKey getKey() {
        return key;
    }

    public SessionState getState() {
        return state;
    }

    public void setState(SessionState state) {
        this.state = state;
    }

    public boolean isLoggedIn() {
        return state == SessionState.LOGGED_IN;
    }

    public PacketSender getPacketSender() {
        return sender;
    }
}
