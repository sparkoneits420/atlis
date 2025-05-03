package org.atlis.client.net;

import java.awt.event.KeyEvent;
import java.io.ByteArrayOutputStream;
import org.atlis.common.security.ISAAC;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.atlis.client.Client;
import org.atlis.common.model.Player;
import org.atlis.common.model.Region;
import org.atlis.common.net.Packet;
import org.atlis.common.net.PacketBuilder;
import org.atlis.common.security.RSA;
import org.atlis.common.util.Constants;
import org.atlis.common.util.Utilities;
import org.atlis.server.Server;

public class Session {

    public Socket socket;
    public DataInputStream in;
    public DataOutputStream out;
    public Queue<PacketBuilder> outgoingPacketQueue;
    public ISAAC encryptor;
    public ISAAC decryptor;
    public Player player;
    public SessionState state;
    public long currentTime, lastExecution;
    public PacketSender sender;
    public HashMap<Long, Region> cachedRegions;
    public String username, password;

    public boolean[] keys;

    public Session() {
        outgoingPacketQueue = new ConcurrentLinkedQueue<>();
        cachedRegions = new HashMap<>();
        keys = new boolean[128];
    }

    public void update() {
        if (getState() == SessionState.CONNECTED) {
            boolean running = keys[KeyEvent.VK_SHIFT];

            if (keys[KeyEvent.VK_W] || keys[KeyEvent.VK_UP]) {
                player.setRunning(running);
                getPacketSender().sendMovementRequest(0);
                if (!player.collision) {
                    player.y -= player.isRunning() ? Constants.RUN_SPEED : Constants.WALK_SPEED;
                    player.current = player.walkAnim[0];
                    player.walkDirection = 0;
                    player.moving = true;
                }

            }
            if (keys[KeyEvent.VK_S] || keys[KeyEvent.VK_DOWN]) {
                player.setRunning(running);
                getPacketSender().sendMovementRequest(1);
                if (!player.collision) {
                    player.y += player.isRunning() ? Constants.RUN_SPEED : Constants.WALK_SPEED;
                    player.current = player.walkAnim[1];
                    player.walkDirection = 1;
                    player.moving = true;
                }

            }
            if (keys[KeyEvent.VK_A] || keys[KeyEvent.VK_LEFT]) {
                player.setRunning(running);
                getPacketSender().sendMovementRequest(2);
                if (!player.collision) {
                    player.x -= player.isRunning() ? Constants.RUN_SPEED : Constants.WALK_SPEED;
                    player.current = player.walkAnim[2];
                    player.walkDirection = 2;
                    player.moving = true;
                }

            }
            if (keys[KeyEvent.VK_D] || keys[KeyEvent.VK_RIGHT]) {
                player.setRunning(running);
                getPacketSender().sendMovementRequest(3);
                if (!player.collision) {
                    player.x += player.isRunning() ? Constants.RUN_SPEED : Constants.WALK_SPEED;
                    player.current = player.walkAnim[3];
                    player.walkDirection = 3;
                    player.moving = true;
                }
            }
        }
    }

    public final void connect(String username, String password) {
        try {
            setState(SessionState.CONNECTING);
            socket = new Socket(Constants.HOST, Constants.PORT);
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());

            login(username, password);
        } catch (IOException e) {
            Client.getLog().put("Failed to establish connection: " + e.getMessage());
        }
    }

    public void login(String username, String password) {
        try {
            setState(SessionState.KEY_EXCHANGE);
            getOutputStream().writeByte(0x00);
            getOutputStream().flush();
            int modLen = getInputStream().readUnsignedShort();
            byte[] modBytes = new byte[modLen];
            getInputStream().readFully(modBytes);

            int expLen = getInputStream().readUnsignedShort();
            byte[] expBytes = new byte[expLen];
            getInputStream().readFully(expBytes);

            BigInteger modulus = new BigInteger(modBytes);
            BigInteger exponent = new BigInteger(expBytes);
            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));
            ByteArrayOutputStream payload = new ByteArrayOutputStream();
            DataOutputStream payloadOut = new DataOutputStream(payload);
            long clientSeed = System.currentTimeMillis();

            payloadOut.writeUTF(username);
            payloadOut.writeUTF(password);
            payloadOut.writeLong(clientSeed);
            byte[] encryptedLoginBlock = RSA.encrypt(payload.toByteArray(), publicKey);
            setState(SessionState.RESPONSE);
            getOutputStream().writeByte(0x01);
            getOutputStream().writeShort(encryptedLoginBlock.length);
            getOutputStream().write(encryptedLoginBlock);
            getOutputStream().flush();
            byte opcode = getInputStream().readByte();
            //Client.getLog().put("Login response opcode: " + opcode);
            if (opcode == Constants.LOGIN_SUCCESS) {
                getSocket().setTcpNoDelay(true);
                getSocket().setKeepAlive(true);
                long serverSeed = getInputStream().readLong();
                //System.out.println("Server seed: " + serverSeed);
                ISAAC encryptor = new ISAAC(RSA.generateSeedArray(clientSeed));
                ISAAC decryptor = new ISAAC(RSA.generateSeedArray(serverSeed));
                setCiphers(encryptor, decryptor);
                //setSession(session);

                player = new Player();
                player.setUsername(username);
                player.setPassword(password);
                setState(SessionState.CONNECTED);
                sender = new PacketSender(this, encryptor);
                // System.out.println("hmmmmm");

                Server.executor.submit(() -> {
                    while (getState() == SessionState.CONNECTED) {
                        currentTime = System.currentTimeMillis();
                        if (currentTime - lastExecution >= Constants.PARSING_INTERVAL) {
                            handleIO();
                            lastExecution = currentTime;
                            //sender.sendIdlePacket();
                        } else {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException ex) {
                                System.err.println("Could not sleep for session");
                            }
                        }
                    }
                }); 
            } else {
                switch (opcode) {
                    case Constants.LOGIN_FAILURE_INVALID_CREDENTIALS -> {
                        Client.getScreen().loginScreen.setMessage("Invalid username or password.");
                        System.out.println("Invalid username or password.");
                    }
                    case Constants.PLEASE_REGISTER -> {
                        Client.getScreen().loginScreen.setMessage("Please register at atlis.online");
                    }
                    default -> {
                        Client.getScreen().loginScreen.setMessage("Unknown login failure.");
                        System.out.println("Unknown login failure.");
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Client.getLog().put("Problem handling IO protocol.");
        }

    }

    public void handleIO() {
        try {

            if (socket.isClosed()) {
                Client.getLog().put("socket closed...");

            }
            if (in == null || out == null) {
                System.out.println("In: " + in + ", out: " + out);
                return;
            }

            //sender.sendIdlePacket();
            if (in.available() >= 3) { // Enough to read header
                // System.out.println("reading");
                //in.mark(5); 
                int opcode = (in.readUnsignedByte() - decryptor.next()) & 0xFF;
                int size = in.readUnsignedShort();

                if (in.available() >= size) { // Only if full payload available
                    byte[] payload = new byte[size];
                    in.readFully(payload);
                    Packet packet = new Packet(payload, opcode);
                    PacketManager.process(packet, this);
                } else {
                    in.reset(); // Not enough data yet, wait
                }
            }
            for (int i = 0; i < outgoingPacketQueue.size(); i++) {
                PacketBuilder pb = outgoingPacketQueue.poll();
                out.write(pb.toPacket());
            }
        } catch (EOFException eof) {
            System.out.println("[CLIENT] EOF reached, closing ");
            close();
        } catch (IOException e) {
            e.printStackTrace();
            close();

        }

    }

    public void queuePacket(PacketBuilder packet) {

        for (PacketBuilder b : outgoingPacketQueue) {
            if (Arrays.equals(b.buffer, packet.buffer)) {
                return;
            }
        }
        outgoingPacketQueue.add(packet);
    }

    public void close() {
        System.out.println("[CLIENT] Close called manually. State: " + getState());
        setState(SessionState.DISCONNECTED);
        try {
            in.close();
            out.close();
            socket.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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

    public DataInputStream getInputStream() {
        return in;
    }

    public DataOutputStream getOutputStream() {
        return out;
    }

    public Player getPlayer() {
        return player;
    }

    public SessionState getState() {
        return state;
    }

    public Socket getSocket() {
        return socket;
    }

    public Queue<PacketBuilder> getPacketQueue() {
        return outgoingPacketQueue;
    }

    public void setState(SessionState state) {
        this.state = state;
    }

    public PacketSender getPacketSender() {
        return sender;
    }

    public void setPacketSender(PacketSender sender) {
        this.sender = sender;
    }

    public HashMap<Long, Region> getCachedRegions() {
        return cachedRegions;
    }
}
