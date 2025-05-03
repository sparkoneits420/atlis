
package org.atlis.client.net.packets;
 
import org.atlis.client.net.Session;
import org.atlis.common.net.Packet; 

/**
 *
 * @author smokey
 */
public interface PacketListener {
    
    public void handle(Packet p, Session session); 
    
}
