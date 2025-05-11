package org.atlis.client;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image; 
import java.util.ArrayList;
import org.atlis.common.tsk.Task;

import java.util.HashMap;
import javax.swing.JPanel;
import org.atlis.client.lstn.KeyManager;
import org.atlis.client.lstn.MouseManager;
import org.atlis.client.net.Session;
import org.atlis.client.net.SessionState; 
import org.atlis.client.ui.LoginScreen;

import java.awt.Font;
import java.awt.RenderingHints;
import java.util.List; 
import org.atlis.client.ui.ChatInterface;
import org.atlis.common.model.GameObject;
import org.atlis.common.model.Player;
import org.atlis.common.model.Region;
import org.atlis.common.model.Tile;
import org.atlis.common.tsk.TaskPool;
import org.atlis.common.util.Constants;
import org.atlis.common.util.Log;

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
        //  chat = new ChatInterface();
        TaskPool.add(new Task(Constants.PAINT_INTERVAL) {
            @Override
            public void execute() {
                repaint();
            }
        });
    }

    @Override
    public void paintComponent(Graphics g) {
        //Log.print("repaint");
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
            // if (player == null) {
            //  session.setState(SessionState.DISCONNECTED);
            //}
            loginScreen.paint(g2);
        } else if (session.getState() == SessionState.CONNECTED) {
            Player player = session.getPlayer();
            if(player == null) { 
                Log.print("player == null / Screen.java client side");
                return;
            }
            /**
             * Paint game
             */
            g2.translate((getWidth() / 2) - player.getX(),
                    (getHeight() / 2) - player.getY());
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);

            if (player.requiresRegionalUpdate()) {
                List<Long> visible = player.getVisibleRegions();

                for (long id : visible) {
                    Log.print(String.valueOf(id));
                    HashMap<Long, Region> regions = session.getCachedRegions();
                    if (regions.get(id) == null) {
                        session.getPacketSender().sendRegionRequest(id);
                    } else {
                        player.getCurrentRegions().put(id, regions.get(id));
                    }
                }
                if (player.region == null) {
                    //Log.print("lol2");
                    return;
                }
            }

            if (player.getCurrentRegions().isEmpty()) {
                //Log.print("lol3");
            }

            for (Region region : new ArrayList<>(player.getCurrentRegions().values())) { 
                for (Tile tile : region.values()) { 
                    g2.drawImage(tile.getImage(), tile.x, tile.y, this);
                } 

                for (GameObject object : new ArrayList<>(region.objects)) {
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

            if (player == null
                    || player.getRegion() == null) {
                return;
            }
            
            /**
             * Paint player
             */
            if (player.moving) {
                if (player.animationCycle >= player.current.images.length) {
                    player.animationCycle = 0;
                }
                g2.drawImage(player.current.images[player.animationCycle++],
                        player.getX() - 16, player.getY() - 16, this);
            } else {
                g2.drawImage(player.idle,
                        player.getX() - 16, player.getY() - 16, this);
            } 

            g2.setColor(Color.BLACK);
            g2.setFont(new Font(Constants.FONT, Font.PLAIN, 12));
            g2.drawString(player.getUsername(), player.x - g2.getFontMetrics().stringWidth(player.getUsername()) / 2, player.y - 20);


            // Draw remote players
            for (Player p : session.getPlayers().values()) {
                g2.drawImage(p.idle, p.getX(), p.getY(), null);
            }

            
            /**
             * Paint player and object boundries if debugging
             */
            if (debug) { 
                g2.setColor(Color.red);
                g2.drawRect(player.getX() - 16, player.getY() - 16, 32, 32);
                for (GameObject obj : player.getRegion().getObjects()) {
                    g2.setColor(Color.blue);
                    g2.drawRect(obj.getX(), obj.getY(), obj.getWidth(), obj.getHeight()); 
                }
                g2.setColor(Color.BLACK);
                g2.setFont(new Font(Constants.FONT, Font.PLAIN, 12));
                g2.drawString("Region: " + player.getCurrentRegionId(), player.x - (getWidth() / 2) + 20, player.y - getHeight() / 2 + 20);
                g2.drawString("X:" + player.x + ", Y: " + player.y, player.x - (getWidth() / 2) + 20, player.y - getHeight() / 2 + 40);
                

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
