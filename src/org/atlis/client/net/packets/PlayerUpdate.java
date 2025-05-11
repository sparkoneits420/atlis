package org.atlis.client.net.packets;
 
import org.atlis.client.net.Session;
import org.atlis.common.model.Player;
import org.atlis.common.model.Region;
import org.atlis.common.model.UpdateFlag;
import org.atlis.common.net.Packet;

public class PlayerUpdate implements PacketListener {

    @Override
    public void handle(Packet p, Session session) {
        long id = p.getLong();
        int x = p.getInt();
        int y = p.getInt();
        byte flagByte = p.getByte();

        UpdateFlag flag = UpdateFlag.getFlag(flagByte);
        if (flag == null) return;
 
        Player local = session.getPlayer();
        Region region = local.region;
        if (region == null) return;

        // Skip self
        if (local.getId() == id)
            return;

        Player remote = session.getPlayer(id);
        if (remote == null) { 
            remote = new Player();
            remote.setId(id);
            remote.x = x;
            remote.y = y;
            session.addPlayer(remote);
        } else {
            if (flag == UpdateFlag.WALKING) {
                remote.x = x;
                remote.y = y;
            } 
            // Future: handle APPEARANCE, REGION, ANIMATION, etc.
        }
    }
}