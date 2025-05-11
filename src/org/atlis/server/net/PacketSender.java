 
package org.atlis.server.net;

import java.util.ArrayList;
import org.atlis.common.model.GameObject; 
import org.atlis.common.model.Player;
import org.atlis.common.model.Region;
import org.atlis.common.model.Tile; 
import org.atlis.common.net.PacketBuilder;
import org.atlis.common.security.ISAAC;
import org.atlis.common.util.Constants;
import org.atlis.common.util.Log;
import org.atlis.common.util.XMLPersistence;
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
        Log.print(String.valueOf(regionId));
        Player player = session.getPlayer(); 
        Region region = Server.getCachedRegions().get(regionId); 

        if (region == null)  {
            region = (Region) XMLPersistence.loadXML(Constants.CACHE_DIR + "/mapdata/" + regionId + ".xml"); 
            if(region != null) Server.getCachedRegions().putIfAbsent(regionId, region);
        }
        if (region == null) {
            Log.print("No region found to send: " + regionId);
            return;
        } 
        if(player.withinRegion(regionId))
            player.setRegion(region);
        
        PacketBuilder packet = new PacketBuilder(0x01, isaac);
        
        packet.addInt(player.x);
        packet.addInt(player.y);
        
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
            //Log.print("Width: " + object.getWidth() + ", Height: " + object.getHeight());
            packet.addInt(object.getWidth());
            packet.addInt(object.getHeight());
            packet.addByte((byte) object.dirs.length);
            for (String dir : object.dirs) {
                packet.addString(dir);
            }
            packet.addByte((byte) (object.animated == true ? 1 : 0));
        }
        //Log.print("Region sent");
        session.queuePacket(packet);
    }

    public void sendMovementResponse(boolean response) {
        PacketBuilder p = new PacketBuilder(0x04, session.getEncryptor()); // MovementResponse
        p.addBoolean(response);
        session.queuePacket(p); 
    } 
}
