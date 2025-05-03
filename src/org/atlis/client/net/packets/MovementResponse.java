package org.atlis.client.net.packets;

import org.atlis.client.net.Session;
import org.atlis.common.model.Player;
import org.atlis.common.model.Region;
import org.atlis.common.net.Packet;
import org.atlis.common.util.Constants;

/**
 *
 * @author smokey
 */
public class MovementResponse implements PacketListener {

    @Override
    public void handle(Packet p, Session session) {
        boolean collision = p.getBoolean();
        System.out.println("Collision response sent by server: " + collision);
        session.getPlayer().collision = collision;
    }
}
