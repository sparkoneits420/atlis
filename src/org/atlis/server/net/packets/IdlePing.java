
package org.atlis.server.net.packets;

import org.atlis.common.net.Packet;
import org.atlis.common.net.PacketBuilder;
import org.atlis.server.net.Session;

/**
 *
 * @author smokey
 */
public class IdlePing implements PacketListener {

    @Override
    public void handle(Packet p, Session session) {
       // System.out.println("idle packet handled");
    } 
}
