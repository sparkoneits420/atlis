package org.atlis.client;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import org.atlis.common.tsk.Task;

import java.util.HashMap;
import javax.swing.JPanel;
import org.atlis.client.lstn.KeyManager;
import org.atlis.client.lstn.MouseManager;
import org.atlis.client.net.Session;
import org.atlis.client.net.SessionState;
import org.atlis.client.ui.Component;
import org.atlis.client.ui.LoginScreen;

import java.awt.Font;
import java.awt.RenderingHints;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.atlis.client.ui.ChatInterface;
import org.atlis.common.model.GameObject;
import org.atlis.common.model.Player;
import org.atlis.common.model.Region;
import org.atlis.common.model.Tile;
import org.atlis.common.util.Constants;

public class Screen extends JPanel {

    public Region currentRegion = null;
    public long currentRegionId = -1;

    public static boolean loaded = false, loggedIn = false, debug = true;

    public KeyManager keyManager;
    public MouseManager mouseManager;
    public LoginScreen loginScreen; 
    public Session session;
    public boolean regionRequested;
    public ChatInterface chat;
    
    public Screen() {
        super();
        initComponents();
    }

    public final void initComponents() {

        session = new Session();
        keyManager = new KeyManager(session);
        mouseManager = new MouseManager(this);
        loginScreen = new LoginScreen(this);
        chat = new ChatInterface();   
        Client.getTaskPool().add(new Task(Constants.PAINT_INTERVAL) {
            @Override
            public void execute() {
                repaint();
                session.update();
            }
        });
    }

    @Override
    public void paintComponent(Graphics g) {
        //System.out.println("repaint");
        Image image = this.createImage(getWidth(), getHeight());
        Graphics2D g2 = (Graphics2D) image.getGraphics();
        /**
         * Loading screen
         */
        if (!loaded) {
            loaded = true;
            /**
             * Login screen
             */
        } else if (session.getState() != SessionState.CONNECTED) {
            // if (session.getPlayer() == null) {
            //  session.setState(SessionState.DISCONNECTED);
            //}
            loginScreen.paint(g2);
        } else if (session.getState() == SessionState.CONNECTED) {

            /**
             * Paint game
             */
            g2.translate((getWidth() / 2) - session.getPlayer().getX(),
                    (getHeight() / 2) - session.getPlayer().getY());
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);

            if (session.getPlayer().requiresRegionalUpdate()) {
                List<Long> visible = session.getPlayer().getVisibleRegions();
                for (long id : visible) {
                    HashMap<Long, Region> regions = session.getCachedRegions();
                    if (regions.get(id) == null) {
                        session.getPacketSender().sendRegionRequest(id);
                    } else {
                        session.getPlayer().getCurrentRegions().put(id, regions.get(id));
                    }
                }
                if (session.getPlayer().region == null) {
                    //System.out.println("lol2");
                    return;
                } 
            }
            
            if(session.getPlayer().getCurrentRegions().isEmpty()) {
            //System.out.println("lol3");
            }

            for (Region region : new ArrayList<>(session.getPlayer().getCurrentRegions().values())) {
                //System.out.println("lol");
                for (Tile tile : region.values()) {
                    //if(tile.getImage() == null) 
                    //System.out.println("image:  " + tile.getImage() + ", x: " + tile.x + " y: " + tile.y);
                    g2.drawImage(tile.getImage(), tile.x, tile.y, this);
                }
            }

            if(session.getPlayer() == null 
                    || session.getPlayer().getRegion() == null) 
                return; 
            
            if (!session.getPlayer().getRegion().objects.isEmpty() 
                    && session.getPlayer().getRegion().objects != null) {
                for (GameObject object : new ArrayList<>(session.getPlayer().getRegion().objects)) {
                    if (object.images == null) {
                        object.loadImages(); 
                    }
                    if (object.currentImageSlot < object.images.length - 1) {
                        object.currentImageSlot++;
                    } else {
                        object.currentImageSlot = 0;
                    }
                    g2.drawImage(object.images[object.currentImageSlot], object.getX(), object.getY(), this);
                }
            }

            /**
             * Paint NPCs
             *//*
            if (!session.getPlayer().getRegion().npcs.isEmpty() && session.getPlayer().getRegion().npcs != null) {
                for (NPC npc : session.getPlayer().getRegion().npcs) {
                    if (npc == null) {
                        continue;
                    }
                    g.drawImage(npc.currentAnim.images[npc.animationCycle++],
                            npc.location.getX() - 16, npc.location.getY() - 16, Client.screen);
                }
            }*/

            /**
             * Paint player
             */
            if (session.getPlayer().moving) {
                if (session.getPlayer().animationCycle >= session.getPlayer().current.images.length) {
                    session.getPlayer().animationCycle = 0;
                }
                g2.drawImage(session.getPlayer().current.images[session.getPlayer().animationCycle++],
                        session.getPlayer().getX() - 16, session.getPlayer().getY() - 16, this);
            } else {
                g2.drawImage(session.getPlayer().idle,
                        session.getPlayer().getX() - 16, session.getPlayer().getY() - 16, this);
            }

            Player player = session.getPlayer();
            if (session.getPlayer() != null) {
                g2.setColor(Color.BLACK);
                g2.setFont(new Font(Constants.FONT, Font.PLAIN, 12));
                g2.drawString(player.getUsername(), player.x - g2.getFontMetrics().stringWidth(player.getUsername()) / 2, player.y - 20);
            }

            if (currentRegion != null) {
                g2.setColor(Color.BLACK);
                g2.setFont(new Font(Constants.FONT, Font.PLAIN, 10)); 
                g2.drawString("Region: " + currentRegionId, player.x - (getWidth() / 2) + 40, player.x - getWidth() / 2 + 40);
            }

            /**
             * Paint player and object boundries if debugging
             */
            if (debug) {
                g2.setColor(Color.red);
                g2.drawRect(session.getPlayer().getX() - 16, session.getPlayer().getY() - 16, 32, 32);
                for (GameObject obj : session.getPlayer().getRegion().getObjects()) {
                    g2.setColor(Color.blue);
                    g2.drawRect(obj.getX(), obj.getY(), obj.getWidth(), obj.getHeight());

                }
            }
            
            /**
             * Draw chat and other interfaces
             */
           // chat.paint(g2);
            
        }
        g.drawImage(image, 0, 0, this);
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public Session getSession() {
        return session;
    }

}
