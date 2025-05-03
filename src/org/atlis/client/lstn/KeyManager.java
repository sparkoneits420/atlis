/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.atlis.client.lstn;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import org.atlis.client.Client;
import org.atlis.client.net.Session;
import org.atlis.client.net.SessionState;
import org.atlis.common.model.Player;
import org.atlis.common.util.Constants;

/**
 *
 * @author smokey
 */
public class KeyManager implements KeyListener {

    private final Session session;

    public KeyManager(Session session) {
        this.session = session;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (session.getState() != SessionState.CONNECTED) {
            return;
        }
        int key = e.getKeyCode();
        if (key >= 0 && key < session.keys.length) {
            session.keys[key] = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        Player player = session.getPlayer();
        if (player == null || session.getState() != SessionState.CONNECTED) {
            return;
        }

        if (key >= 0 && key < session.keys.length) {
            session.keys[key] = false;
        }

        if (key == KeyEvent.VK_SHIFT) {
            player.setRunning(false); // Stop running if shift is released
        }

        switch (key) {
            case KeyEvent.VK_W, KeyEvent.VK_UP, KeyEvent.VK_S, KeyEvent.VK_DOWN, KeyEvent.VK_A, KeyEvent.VK_LEFT, KeyEvent.VK_D, KeyEvent.VK_RIGHT -> {
                player.moving = false;
                player.idle = player.walkAnim[player.walkDirection].images[0];
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // No need to handle anything here for movement
    }
}
