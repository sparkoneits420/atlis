package org.atlis.server.net.packets;

import org.atlis.common.model.Player;
import org.atlis.server.net.Session; 

import org.atlis.common.model.UpdateFlag;
import org.atlis.common.net.Packet;
import org.atlis.server.net.SessionPool.PlayerRegistry;
 
public class PlayerUpdate implements PacketListener { 

    @Override
    public void handle(Packet p, Session session) {
        long playerId = p.getLong();
        Player player = PlayerRegistry.get(playerId);
        int size = p.getByte();
        for (int i = 0; i < size; i++) {
            UpdateFlag flag = UpdateFlag.getFlag(p.getByte());
            if(flag != null && !session.getPlayer().getUpdateFlags().contains(flag)) 
                player.addUpdateFlag(flag);
        }
    }
} 