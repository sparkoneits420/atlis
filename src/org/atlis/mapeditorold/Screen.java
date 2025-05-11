package org.atlis.mapeditorold;

import org.atlis.mapeditorold.build.NPCBuilder;
import org.atlis.mapeditorold.build.ObjectBuilder;
import org.atlis.mapeditorold.add.AddNPC;
import org.atlis.mapeditorold.add.AddObject;
import org.atlis.common.tsk.Task;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*; 
import java.util.ArrayList;
import java.util.HashMap;
import org.atlis.common.model.*;
import org.atlis.common.tsk.TaskPool;
import org.atlis.common.util.Constants;
import org.atlis.common.util.Log;
import org.atlis.common.util.XMLPersistence;
import org.atlis.common.util.Utilities;

public class Screen extends JPanel implements MouseMotionListener, MouseListener, ActionListener, KeyListener {

    public Player player;
    public static boolean initialized = false, addingObject = false;
    public static boolean addingNPC;
    public boolean changedLocation;

    public GameObject objectSelected;
    public static NPC npcToAdd;
    public Region region;
    public ArrayList<Tile> selected, modified;
    public static GameObject objectToAdd;
    public HashMap<Long, Region> cachedRegions;
    public int mouseX, mouseY;

    public Screen() {

        addMouseListener(this);
        addMouseMotionListener(this);
        addKeyListener(this);
        modified = new ArrayList<>();
        selected = new ArrayList<>();
        cachedRegions = new HashMap<>();
        player = new Player();
        player.setX(0);
        player.setY(0);
        TaskPool.add(
                new Task(Constants.PAINT_INTERVAL) {
            @Override
            public void execute() {
                repaint();
            }
        });
    }

    @Override
    public void paintComponent(Graphics g) {
        Image image = this.createImage(getWidth(), getHeight());
        Graphics g2 = image.getGraphics();
        if (!initialized) {
            if (region == null) {
                region = XMLPersistence.loadXML(Constants.CACHE_DIR + "/mapdata/" + Utilities.intsToLong(player.x, player.y) + ".xml");
                System.out.println("tried loading region");
            }
            // System.out.println("generating surrounding regions, center null");
            player.sgenerateSurroundingRegions();

            initialized = true;
        } else {
            g2.translate((getWidth() / 2) - player.x, (getHeight() / 2) - player.y);
            if (region == null) { 
                return;
            }
            for (Tile tile : region.values()) {
                if (tile.isVisible(player.x, player.y, this)) {
                    g2.drawImage(tile.getImage(), tile.getX(), tile.getY(), this);
                    g2.setColor(Color.GREEN);
                    g2.drawRect(tile.getX(), tile.getY(), 16, 16);
                }
            }
            int paintedRegions = 0;
            for (Region visRegion : cachedRegions.values()) {
                if (visRegion == region) {
                    continue;
                }
                if (visRegion.isVisible(region)) {
                    paintedRegions++;
                    //MapEditor.getLog().put("painting region");
                    for (Tile tile : visRegion.values()) {
                        if (tile.isVisible(player.x, player.y, this)) {
                            g2.drawImage(tile.getImage(), tile.getX(), tile.getY(), this);
                            g2.setColor(Color.GREEN);
                            g2.drawRect(tile.getX(), tile.getY(), 16, 16);
                        }
                    }
                } else {
                    //System.out.println("region not visible");
                }
            }
            //MapEditor.getLog().put("Painted {" + paintedRegions + "} regions");
            for (Tile tile : selected) {
                g2.setColor(Color.BLACK);
                g2.drawRect(tile.getX(), tile.getY(), 16, 16);
            }
        }

        if (objectToAdd != null && objectToAdd.images != null) {
            if (objectToAdd.currentImageSlot < objectToAdd.images.length - 1) {
                objectToAdd.currentImageSlot++;
            } else {
                objectToAdd.currentImageSlot = 0;
            }

            g2.drawImage(objectToAdd.images[objectToAdd.currentImageSlot], objectToAdd.getX(), objectToAdd.getY(), this);
        }

        if (npcToAdd != null && npcToAdd.idle != null) {
            g2.drawImage(npcToAdd.idle, npcToAdd.getX(), npcToAdd.getY(), this);
        }
        //MapEditor.getLog().put("Objects loaded: " + region.objects.size());
        if (!region.objects.isEmpty() && region.objects != null) {
            for (GameObject object : region.objects) {
                if (object.images == null) {
                    continue;
                }
                if (object.currentImageSlot < object.images.length - 1) {
                    object.currentImageSlot++;
                } else {
                    object.currentImageSlot = 0;
                }
                g2.drawImage(object.images[object.currentImageSlot], object.getX(), object.getY(), this);

            }
        }

        //MapEditor.getLog().put("Objects loaded: " + region.objects.size());
        if (!region.npcs.isEmpty() && region.npcs != null) {
            for (NPC npc : region.npcs) {
                if (npc.idle == null) {
                    continue;
                }
                g2.drawImage(npc.idle, npc.getX(), npc.getY(), this);
                // npc.handleMovement(g);
            }
        }

        if (objectSelected != null) {
            g2.setColor(Color.RED);
            g2.drawRect(objectSelected.getX(), objectSelected.getY(), objectSelected.getWidth(), objectSelected.getHeight());
        }

        //MapEditor.getLog().put(getWidth() + ", " + getHeight());
        g.drawImage(image, 0, 0, this);
    } 
    
    
    public Tile getTile(int x, int y) {

        for (Region aRegion : cachedRegions.values()) {
            for (Tile tile : aRegion.values()) {
                if (tile == null) {
                    continue;
                }
                int j = tile.x + 16, k = tile.y + 16;
                if ((x <= j && x >= tile.x) && (y <= k && y >= tile.y)) {
                    return tile;
                }
            }
        }
        for (Tile tile : selected) {
            if (tile == null) {
                continue;
            }
            int j = tile.x + 16, k = tile.y + 16;
            if ((x <= j && x >= tile.x) && (y <= k && y >= tile.y)) {
                return tile;
            }
        }

        return null;
    }

    
    @Override
    public void mouseClicked(MouseEvent e) {

        if (addingObject) {

        }

        for (GameObject o : region.objects) {
            if (o == null) {
                continue;
            }
            //if (o.withinBounds()) {
              //  objectSelected = o;
                //return;
            //}
        }
        Tile tile = getTile((e.getX() + player.x) - (getWidth() / 2),
                (e.getY() + player.y) - (getHeight() / 2));
        if (tile == null) {
            return;
        }
        if (!selected.contains(tile)) {
            //region.remove(tile);
            selected.add(tile);
        } else if (selected.contains(tile)) {
            selected.remove(tile);
            //region.add(tile);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {
        int x = (e.getX() + player.x) - (getWidth() / 2),
                y = (e.getY() + player.y) - (getHeight() / 2);
        if (addingObject) {
            objectToAdd.setLocation(x - objectToAdd.getWidth() / 2, y - objectToAdd.getHeight() / 2);
            objectToAdd.setBounds();
            return;
        }

        if (addingNPC) {
            npcToAdd.setLocation(x - npcToAdd.width / 2, y - npcToAdd.height / 2);
            npcToAdd.setBounds();
        }

        /*
        for(GameObject o : region.objects) {
            if(o == null) {
                continue;
            }
            if(o.withinBoundry(x, y)) {
                objectSelected = o;
            }
            if(objectSelected != null) {
                o.setLocation(x, y);
                return;
            }
        }
         */
        Tile tile = getTile(x, y);
        if (tile == null) {
            return;
        }
        if (!selected.contains(tile)) {
            selected.add(tile);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    } 

    @Override
    public void actionPerformed(ActionEvent e) {
        switch (e.getActionCommand().toLowerCase()) {
            case "replace" -> {
                Object[] options = {"Grass-0", "Stone-1"};
                Object j = JOptionPane.showInputDialog(this,
                        "Choose a replacement tile.",
                        "", JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                ArrayList<Tile> temp = new ArrayList<>(selected);
                int newType = Integer.parseInt(j.toString().split("-")[1]);

                for (Tile tile : temp) {
                    long tileId = Utilities.intsToLong(tile.x, tile.y);
                    region.remove(tileId);  
                    tile.setType(newType);
                    region.put(tileId, tile);
                }
 
                selected.removeAll(temp);  // <== move deletion here, safely
                temp.clear();
            }
             
            case "log dump" -> {
                Log.dump();
            }

            case "create object" -> {
                ObjectBuilder.open();
            }
            case "add object" -> {
                AddObject.open();
            }

            case "create npc" -> {
                NPCBuilder.open();
            }

            case "add npc" -> {
                AddNPC.open();
            }

            case "save" -> {
                for (Region r : cachedRegions.values()) {
                    XMLPersistence.saveXML(r);
                }
            }

        }
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (!cachedRegions.containsValue(region) && region != null) {
            cachedRegions.put(region.getId(), region);
        }
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP -> {
                player.y -= Constants.REGION_SIZE;
                changedLocation = true;
            }
            case KeyEvent.VK_DOWN -> {
                player.y += Constants.REGION_SIZE;
                changedLocation = true;
            }
            case KeyEvent.VK_LEFT -> {
                player.x -= Constants.REGION_SIZE;
                changedLocation = true;
            }
            case KeyEvent.VK_RIGHT -> {
                player.x += Constants.REGION_SIZE;
                changedLocation = true;
            }
            case KeyEvent.VK_ENTER -> {
                if (objectToAdd != null && addingObject) {
                    region.getObjects().add(objectToAdd);
                    objectToAdd = null;
                    addingObject = false;
                }
                if (npcToAdd != null && addingNPC) {
                    region.getNPCs().add(npcToAdd);
                    npcToAdd = null;
                    addingNPC = false;
                }
                if (objectSelected != null) {
                    objectSelected.setBounds();
                    objectSelected = null;
                }
            }
        }
        if (changedLocation) {
            player.sgenerateSurroundingRegions();
            long regionId = Utilities.intsToLong(player.x, player.y);
            //MapEditor.getLog().put("Region ID: " + regionId);
            int[] ints = Utilities.longToInts(regionId);
            //MapEditor.getLog().put(ints[0] + ", " + ints[1]);
            if (cachedRegions.containsKey(regionId)) {
                //MapEditor.getLog().put("Cached");
                region = cachedRegions.get(regionId);
                //MapEditor.getLog().put("Cached: X: " + player.x + ", Y: " + player.y);
            } else {
                //MapEditor.getLog().put("Not cached");
                region = XMLPersistence.loadXML(Constants.CACHE_DIR + "/mapdata/" + Utilities.intsToLong(ints[0], ints[1]) + ".xml");
                if (region == null) {
                    //MapEditor.getLog().put("Generation new regions because region is null");

                    //generateRegion();
                }
                //cachedRegions.put(region.getId(), region);
            }
        }
        //MapEditor.getLog().put("X: " + player.x + ", Y: " + player.y);

    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

}
