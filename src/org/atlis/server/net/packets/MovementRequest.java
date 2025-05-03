/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.atlis.server.net.packets;

import org.atlis.common.model.Player;
import org.atlis.common.model.UpdateFlag;
import org.atlis.common.net.Packet;
import org.atlis.common.util.Constants;
import org.atlis.server.net.Session;

/**
 *
 * @author smokey
 */
public class MovementRequest implements PacketListener {

    @Override
    public void handle(Packet p, Session session) {
        int x = p.getInt(), y = p.getInt();
        int walkDirection = p.getByte();  
        boolean running = p.getBoolean();

        Player player = session.getPlayer();
        player.running = running;
        
        if (player.region == null || walkDirection < 0 || walkDirection > 3) { 
            session.getPacketSender().sendMovementResponse(false);
            return;
        }

        int walkSpeed = player.isRunning() ? Constants.RUN_SPEED : Constants.WALK_SPEED;
        int newX = x;
        int newY = y;

        System.out.println("Direction: " + walkDirection);
        switch (walkDirection) {
            case 0 ->
                newY -= walkSpeed;
            case 1 ->
                newY += walkSpeed;
            case 2 ->
                newX -= walkSpeed;
            case 3 ->
                newX += walkSpeed;
        }

        boolean canMove = player.canMoveDirection(walkDirection);
        
        if (canMove) { 
            player.x = newX;
            player.y = newY;
            player.addUpdateFlag(UpdateFlag.WALKING);
        }

        session.getPacketSender().sendMovementResponse(!canMove); // true = collision
    }
}
