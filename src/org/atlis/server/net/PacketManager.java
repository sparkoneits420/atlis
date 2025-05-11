package org.atlis.server.net;

import org.atlis.server.net.packets.RegionRequest; 

import java.util.HashMap;
import java.util.Map;
import org.atlis.common.net.Packet;
import org.atlis.common.util.Log;
import org.atlis.server.Server;
import org.atlis.server.net.packets.*;

public final class PacketManager {

    private static final Map<Integer, PacketListener> packets;
    
    static {
        packets = new HashMap<>();
        packets.put(0x02, new RegionRequest());
        packets.put(0x03, new MovementRequest());  
    }

    public static void process(Packet p, Session session) throws Exception {
        if (!session.isLoggedIn()) {
            Log.print("Packet ignored: client not logged in.");
            return;
        }

        PacketListener packet = packets.get(p.getOpcode());
        if (packet != null) {
            packets.get(p.getOpcode()).handle(p, session);
            // Server.getLog().put("Attempted to handle registered packet: " + p.getOpcode());
        } else {
            Log.print("Unhandled packet: " + p.getOpcode() + ", size: " + p.remaining());
        }
    }
}
