package org.atlis.server.net;
 
import org.atlis.common.util.Constants;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey; 
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher; 
import org.atlis.common.model.Player;
import org.atlis.common.security.ISAAC;
import org.atlis.common.security.RSA;  
import org.atlis.common.tsk.TaskPool;
import org.atlis.common.util.Log;
import org.atlis.server.Server;
import org.atlis.server.net.SessionPool.PlayerRegistry;  
import org.atlis.server.net.sql.Database; 

public class LoginProtocol {

    private static final Map<SocketChannel, KeyPair> keyPairs = new HashMap<>();

    public static boolean handle(Session session, ByteBuffer buffer) throws Exception {
        SocketChannel channel = session.getChannel();

        session.setState(SessionState.PROTOCOL);

        byte[] encrypted = new byte[session.getLoginBlockLength()];
        buffer.get(encrypted);

        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, keyPairs.get(channel).getPrivate());
        byte[] decrypted = cipher.doFinal(encrypted);
        byte responseOpcode = Constants.LOGIN_SUCCESS;
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(decrypted));
        String username = dis.readUTF();
        //Log.print("Login attempt: " + username);
        String pass = dis.readUTF();
        Player player = null;
        if (Database.playerExists(username)) {
            player = Database.loadPlayer(username, pass);
            if (player == null) {
                responseOpcode = Constants.LOGIN_FAILURE_INVALID_CREDENTIALS;
                Log.print("Invalid login credentials for player: " + username);
            }
        } else {
            responseOpcode = Constants.PLEASE_REGISTER;
        }
        boolean success = (responseOpcode == Constants.LOGIN_SUCCESS);
        long clientSeed = dis.readLong();
        ByteBuffer loginResponse = ByteBuffer.allocate(17);
        long serverSeed = System.nanoTime();
        if (success) {
            session.setCiphers(
                    new ISAAC(RSA.generateSeedArray(serverSeed)),
                    new ISAAC(RSA.generateSeedArray(clientSeed))
            ); 
            session.setPlayer(player);
            session.sender = new PacketSender(session, session.encryptor);
            TaskPool.add(session);
            PlayerRegistry.register(session.getPlayer());
            
        } 
        if(player == null) {
            session.close();
            return false;
        }
        loginResponse.put(responseOpcode);
        loginResponse.putLong(serverSeed);
        //player.setX(50);
        //player.setY(50);
        loginResponse.putInt(player.getX());
        loginResponse.putInt(player.getY());
        loginResponse.flip();
        channel.write(loginResponse);
        loginResponse.clear(); 

        return true;
    }

    public static void exchangeKeys(Session session) {
        try {
            SocketChannel channel = session.getChannel();

            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(512);
            KeyPair kp = kpg.generateKeyPair();
            keyPairs.put(channel, kp);

            RSAPublicKey pubKey = (RSAPublicKey) kp.getPublic();
            byte[] modBytes = pubKey.getModulus().toByteArray();
            byte[] expBytes = pubKey.getPublicExponent().toByteArray();

            ByteBuffer buffer = ByteBuffer.allocate(2 + modBytes.length + 2 + expBytes.length);
            buffer.putShort((short) modBytes.length);
            buffer.put(modBytes);
            buffer.putShort((short) expBytes.length);
            buffer.put(expBytes);
            buffer.flip();  

            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            session.setState(SessionState.PROTOCOL); // ✅ Now ready to read login block
        } catch (IOException | NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
    }
}
