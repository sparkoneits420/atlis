package org.atlis.server.net.packets;

import org.atlis.common.model.UpdateFlag;
import org.atlis.common.net.Packet;
import org.atlis.server.Server;
import org.atlis.server.net.Session;

/**
 *
 * @author smokey
 */
public class RegionRequest implements PacketListener {

    @Override
    public void handle(Packet p, Session session) {
        long regionId = p.getLong();
        //Server.getLog().put("Region requested: " + regionId);
        session.getPlayer().getUpdateFlags().add(UpdateFlag.REGION);
        session.getPacketSender().sendRequestedRegion(regionId);
    }
}
