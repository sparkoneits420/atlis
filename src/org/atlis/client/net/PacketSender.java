/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package org.atlis.client.net;
 
import org.atlis.common.model.Player;
import org.atlis.common.model.UpdateFlag;
import org.atlis.common.net.PacketBuilder;
import org.atlis.common.security.ISAAC;

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
    
    public void sendIdlePacket() { 
         session.queuePacket(new PacketBuilder(0x55, isaac));
    }
    
    public void sendRegionRequest(long id) {
        PacketBuilder p = new PacketBuilder(0x02, isaac); 
        p.addLong(id);  
        //System.out.println("Sent RegionRequest for ID: " + id); 
        session.queuePacket(p);
    }
    
    public void sendMovementRequest(int direction) {
        PacketBuilder p = new PacketBuilder(0x03, isaac);
        p.addInt(session.getPlayer().x);
        p.addInt(session.getPlayer().y);
        p.addByte((byte)(direction));
        p.addBoolean(session.getPlayer().isRunning());
        session.queuePacket(p);
    }
    
    public void sendUpdateRequest(Player player) {
        PacketBuilder p = new PacketBuilder(0x05, isaac); 
        p.addLong(player.getId());
        p.addByte((byte) player.getUpdateFlags().size());
        for(UpdateFlag flag : player.getUpdateFlags()) {
            p.addByte((byte) flag.getBit());
        }
    }
}
