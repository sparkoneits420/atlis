package org.atlis.client.net;
  
 
import org.atlis.client.net.packets.RegionData; 
import java.util.HashMap;
import java.util.Map;
import org.atlis.client.Client;
import org.atlis.common.net.Packet;  
import org.atlis.client.net.packets.*;
import org.atlis.common.util.Log;

public final class PacketManager {

    public static final Map<Integer,  PacketListener> packets;

    static { 
        packets = new HashMap<>(256);
        packets.put(0x01, new RegionData());
        packets.put(0x04, new MovementResponse());
        packets.put(0x06, new PlayerUpdate());
        packets.put(0x07, new RemovePlayer());
    } 

    public static void process(Packet p, Session session) {
        if (session.getState() != SessionState.CONNECTED) {
            Log.print("Packet ignored: not connected");
            return;
        } 

        PacketListener packet = packets.get(p.getOpcode());
        if(packet != null) { 
            packet.handle(p, session);
            //Client.getLog().put("Handled packet: " + p.getOpcode());
        } else 
            Log.print("Unhandled packet: " + p.getOpcode() + ", size: " + p.remaining());
 
    }
}