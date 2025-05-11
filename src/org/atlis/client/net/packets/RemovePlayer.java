package org.atlis.client.net.packets;
 
import org.atlis.client.net.Session;
import org.atlis.common.model.Player;
import org.atlis.common.model.Region;
import org.atlis.common.net.Packet;
import org.atlis.common.util.Log;

/**
 * Handles removal of a player from the current region's player list.
 * Triggered when a player moves out of view or disconnects.
 *
 * @author smokey
 */
public class RemovePlayer implements PacketListener {

    @Override
    public void handle(Packet p, Session session) {
        long playerId = p.getLong();
 
        Region region = session.getPlayer().region;
        if (region == null) return;

        Player removed = session.getPlayer(playerId);
        if (removed != null) {
            session.removePlayer(playerId);
            Log.print("Removed player " + playerId + " from region.");
        }
    }
}
