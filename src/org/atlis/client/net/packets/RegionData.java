package org.atlis.client.net.packets;
 
import org.atlis.client.net.Session;
import org.atlis.common.model.GameObject;
import org.atlis.common.model.Region; 
import org.atlis.common.net.Packet; 
import org.atlis.common.util.Utilities;

/**
 * Handles an incoming region data supply packet
 * 
 * @author smokey
 */ 
public class RegionData implements PacketListener {

    private Region region;

    @Override
    public void handle(Packet p, Session session) {
        long regionId = p.getLong();
        int tileLength = p.getShort();
        int[] xy = Utilities.longToInts(regionId);
        region = new Region(xy[0], xy[1]); 
        for(int i = 0; i < tileLength; i++) {
            int x = p.getInt(), y = p.getInt();
            region.get(Utilities.intsToLong(x, y))
                    .setType(p.getByte());
        } 
        if(session.getPlayer().getVisibleRegions().contains(regionId)) { 
            session.getPlayer().getCurrentRegions().put(regionId, region);
        }
        
        if(session.getPlayer().withinRegion(regionId))
            session.getPlayer().setRegion(region);
        session.getCachedRegions().put(regionId, region);
        int count = p.getByte();
        for(int i = 0; i < count; i++) {
            int x = p.getInt();
            int y = p.getInt();
            int width = p.getInt();
            int height = p.getInt();
            System.out.println(width + ", " + height);
            int length = p.getByte();
            String[] dirs = new String[length];
            for(int j = 0; j < length; j++) {
                dirs[j] = p.getString();
            }
            boolean animated = p.getByte() == 1;
            GameObject object = new GameObject(x, y, width, height, animated, dirs);
            region.addObject(object);
        } 
    } 
}