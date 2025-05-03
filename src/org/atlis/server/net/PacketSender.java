
package org.atlis.server.net;

import java.util.ArrayList;
import org.atlis.common.model.GameObject;
import org.atlis.common.model.Player;
import org.atlis.common.model.Region;
import org.atlis.common.model.Tile;
import org.atlis.common.model.UpdateFlag;
import org.atlis.common.net.PacketBuilder;
import org.atlis.common.security.ISAAC;
import org.atlis.server.Server;

/**
 *
 * @author smokey
 */
public class PacketSender {
    
    public Session session;
    public ISAAC isaac;

    public PacketSender(Session session, ISAAC isaac) {
        this.session = session;
        this.isaac = isaac; 
    }

    public void sendRequestedRegion(long regionId) {
        //System.out.println("Sending region");
        //if(!session.getPlayer().regionRequested)
            session.getPlayer().generateSurroundingRegions();

        Region region = Server.getCachedRegions().get(regionId); 
        if(session.getPlayer().withinRegion(regionId))
            session.getPlayer().setRegion(region);
        if (region == null) {
            System.out.println("No region found to send.");
            return;
        }

        PacketBuilder packet = new PacketBuilder(0x01, isaac);

        packet.addLong(region.getId());
        ArrayList<Tile> nonDefTiles = new ArrayList<>();
        for (Tile tile : region.values()) {
            if (tile.getType() != Tile.GRASS && !nonDefTiles.contains(tile)) {
                nonDefTiles.add(tile);
            }
        }
        packet.addShort((short) nonDefTiles.size());
        for (Tile tile : nonDefTiles) {
            packet.addInt(tile.getX());
            packet.addInt(tile.getY());
            packet.addByte((byte) tile.getType());
        }
        packet.addByte((byte) region.getObjects().size());
        for (GameObject object : region.getObjects()) {
            packet.addInt(object.getX());
            packet.addInt(object.getY());
            System.out.println("Width: " + object.getWidth() + ", Height: " + object.getHeight());
            packet.addInt(object.getWidth());
            packet.addInt(object.getHeight());
            packet.addByte((byte) object.dirs.length);
            for (String dir : object.dirs) {
                packet.addString(dir);
            }
            packet.addByte((byte) (object.animated == true ? 1 : 0));
        }
        //System.out.println("Region sent");
        session.queuePacket(packet);
    }

    public void sendMovementResponse(boolean response) {
        PacketBuilder p = new PacketBuilder(0x04, session.getEncryptor()); // MovementResponse
        p.addBoolean(response);
        session.queuePacket(p); 
    } 
}
