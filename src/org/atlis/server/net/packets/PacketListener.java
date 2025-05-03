
package org.atlis.server.net.packets;
 
import org.atlis.common.net.Packet;
import org.atlis.server.net.Session;

public interface PacketListener {
    
    public void handle(Packet p, Session session);
            
}
