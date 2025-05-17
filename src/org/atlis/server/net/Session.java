package org.atlis.server.net;
 
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.atlis.common.model.Player; 
import org.atlis.common.model.UpdateFlag;
import static org.atlis.common.model.UpdateFlag.*;
import org.atlis.common.net.Packet;
import org.atlis.common.net.PacketBuilder;
import org.atlis.common.security.ISAAC;
import org.atlis.common.tsk.Task;
import org.atlis.common.tsk.TaskPool;
import org.atlis.common.util.Constants;
import org.atlis.common.util.Log; 
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

    public final ByteBuffer packetBuffer = ByteBuffer.allocate(256); // incoming buffer
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
                if(pb == null) continue;
                Log.print("Attempting to handle outgoing packet: " + pb.opcode);
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
            Log.print("IOException:", e);
            close();
        }
    }

    public void update() {

        if (player == null || player.region == null) {
            return;
        }

        long myRegionId = player.getCurrentRegionId();

        if (player.regionChanged()) {
            for (Player other : PlayerRegistry.all().values()) {
                if (other == player || other.region == null) {
                    continue;
                }

                long otherRegionId = other.getCurrentRegionId();

                boolean wasInOldRegion = player.lastRegionId == otherRegionId;
                boolean inNewRegion = myRegionId == otherRegionId;

                if (wasInOldRegion && !inNewRegion) {
                    // Player moved out of view — remove
                    PacketBuilder pb = new PacketBuilder(0x07, encryptor);
                    pb.addLong(other.getId());
                    queuePacket(pb);
                }

                if (!wasInOldRegion && inNewRegion) {
                    // Player moved into view — add/update
                    PacketBuilder pb = new PacketBuilder(0x05, encryptor);
                    pb.addLong(other.getId());
                    pb.addInt(other.getX());
                    pb.addInt(other.getY());
                    pb.addByte((byte) UpdateFlag.REGION.getBit());
                    queuePacket(pb);
                }
            }
        }

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

                PacketBuilder pb = new PacketBuilder(6, encryptor); // PlayerUpdate opcode
                pb.addLong(player.getId());
                pb.addInt(player.getX());
                pb.addInt(player.getY());
                pb.addByte((byte) WALKING.getBit());
                otherSession.queuePacket(pb);
            }

        }
        player.clearUpdateFlags();
        // Future: handle other flags (APPEARANCE, CHAT, etc.)
    }

    private void updateVisibility() {
        long currentId = player.getRegionId();
        long lastId = player.getLastRegionId();

        if (currentId == lastId) {
            return;
        }

        for (Player other : PlayerRegistry.all().values()) {
            if (other == player || other.region == null) {
                continue;
            }

            long otherId = other.getRegionId();

            // Tell *this* player about others
            if (lastId == otherId && currentId != otherId) {
                PacketBuilder pb = new PacketBuilder(0x07, encryptor);
                pb.addLong(other.getId());
                queuePacket(pb);
            } else if (lastId != otherId && currentId == otherId) {
                PacketBuilder pb = new PacketBuilder(0x05, encryptor);
                pb.addLong(other.getId());
                pb.addInt(other.getX());
                pb.addInt(other.getY());
                pb.addByte((byte) REGION.getBit());
                queuePacket(pb);
            }

            // Now tell others about *this* player
            Session otherSession = PlayerRegistry.getSessionByPlayer(other);
            if (otherSession == null) {
                continue;
            }

            if (other.getLastRegionId() == currentId && other.getRegionId() != currentId) {
                // Remove this player from their view
                PacketBuilder pb = new PacketBuilder(0x07, otherSession.encryptor);
                pb.addLong(player.getId());
                otherSession.queuePacket(pb);
            } else if (other.getLastRegionId() != currentId && other.getRegionId() == currentId) {
                // Add this player to their view
                PacketBuilder pb = new PacketBuilder(0x05, otherSession.encryptor);
                pb.addLong(player.getId());
                pb.addInt(player.getX());
                pb.addInt(player.getY());
                pb.addByte((byte) REGION.getBit());
                otherSession.queuePacket(pb);
            }
        }
    }

    public void read(SelectionKey key, ByteBuffer buffer) {
        try {
            if (getState() == SessionState.KEY_EXCHANGE || getState() == SessionState.PROTOCOL) {
                int bytesRead = channel.read(loginBuffer);
                if (bytesRead == -1) {
                    Log.print("Client closed during login: " + channel.getRemoteAddress());
                    SessionPool.unregister(channel);
                    key.cancel();
                    return;
                }

                loginBuffer.flip();

                if (loginReadStage == 0 && loginBuffer.remaining() >= 1) {
                    byte hello = loginBuffer.get();
                    if (hello != 0x00) {
                        Log.print("Unexpected hello byte: " + hello);
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
                    loginBuffer.clear();
                    return;
                }

                loginBuffer.compact();
                return;
            }

            if (getState() == SessionState.LOGGED_IN) {
                buffer.clear();
                int bytesRead = channel.read(buffer);
                //Log.print("bytes read after login: " + bytesRead);
                if (bytesRead == 0) {
                    // No data available yet, return safely
                    return;
                }

                if (bytesRead == -1) {
                    // Log.print("Disconnected after login: " + channel.getRemoteAddress());
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
            Log.print("Session dropped for " + player.getUsername());
            Log.print("Session exception: ", e);
            close();
        }
    }

    public void close() {
        try {
            key.cancel();
            channel.close();
            Database.savePlayer(player);
            TaskPool.remove(this);
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
