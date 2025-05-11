// Screen.java - Editor canvas for map visualization and interaction
package org.atlis.mapeditor;

import org.atlis.common.model.*;
import org.atlis.common.tsk.Task;
import org.atlis.common.tsk.TaskPool;
import org.atlis.common.util.Constants;
import org.atlis.common.util.Utilities;
import org.atlis.common.util.XMLPersistence;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;

public class Screen extends JPanel implements KeyListener, MouseListener, MouseMotionListener {

    public static final int TILE_TYPE_DEFAULT = Tile.GRASS;

    public final HashMap<Long, Region> cachedRegions = new HashMap<>();
    public final java.util.Set<Long> modifiedRegionIds = new java.util.HashSet<>();
    public final java.util.Deque<Runnable> undoStack = new java.util.ArrayDeque<>();
    public final java.util.Deque<Runnable> redoStack = new java.util.ArrayDeque<>();
    public final int MAX_HISTORY = 15;
    public int cameraX = 0, cameraY = 0;
    public int tileSize = 16;

    public boolean up, down, left, right;
    public long lastMoveTime = 0;
    public int holdCounter = 0;

    public int hoverX = -1, hoverY = -1;
    public int selectedX = -1, selectedY = -1;
    public final HashMap<Long, Tile> selectedTiles = new HashMap<>();
    public GameObject previewObject = null;
    public boolean placingObject = false;

    public Screen() {
        setFocusable(true);
        setDoubleBuffered(true);
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);

        TaskPool.add(new Task(16) {
            @Override
            public void execute() {
                long now = System.currentTimeMillis();
                if (now - lastMoveTime >= 16) {
                    int speed = Math.min(12, 2 + holdCounter / 10);
                    if (up) {
                        cameraY -= speed;
                    }
                    if (down) {
                        cameraY += speed;
                    }
                    if (left) {
                        cameraX -= speed;
                    }
                    if (right) {
                        cameraX += speed;
                    }

                    if (up || down || left || right) {
                        holdCounter++;
                        repaint();
                    } else {
                        holdCounter = 0;
                    }
                    loadVisibleRegions();
                    lastMoveTime = now;
                }
            }
        });
    }

    public void loadVisibleRegions() {
        int screenW = getWidth();
        int screenH = getHeight();

        int xStart = cameraX;
        int yStart = cameraY;
        int xEnd = cameraX + screenW;
        int yEnd = cameraY + screenH;

        int regionSize = Constants.REGION_SIZE;
        int rxStart = Math.floorDiv(xStart, regionSize) * regionSize;
        int ryStart = Math.floorDiv(yStart, regionSize) * regionSize;
        int rxEnd = Math.floorDiv(xEnd, regionSize) * regionSize;
        int ryEnd = Math.floorDiv(yEnd, regionSize) * regionSize;

        for (int x = rxStart; x <= rxEnd; x += regionSize) {
            for (int y = ryStart; y <= ryEnd; y += regionSize) {
                long id = Utilities.intsToLong(x, y);
                loadOrCreateRegion(id);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawRegions(g2);

        if (hoverX >= 0 && hoverY >= 0) {
            g2.setColor(Color.YELLOW);
            g2.drawRect(hoverX * tileSize - cameraX, hoverY * tileSize - cameraY, tileSize, tileSize);
        }

        for (Tile tile : selectedTiles.values()) {
            int drawX = tile.getX() - cameraX;
            int drawY = tile.getY() - cameraY;
            g2.setColor(Color.CYAN);
            g2.drawRect(drawX, drawY, tileSize, tileSize);
        }

        if (previewObject != null && previewObject.images != null && previewObject.images.length > 0) {
            int px = hoverX * tileSize - cameraX;
            int py = hoverY * tileSize - cameraY;
            g2.drawImage(previewObject.images[previewObject.currentImageSlot], px, py, null);
        }
    }

    public void drawRegions(Graphics2D g2) {
        for (Region region : cachedRegions.values()) {
            
            // --- Draw Tiles ---
            for (Tile tile : region.values()) {
                int drawX = tile.getX() - cameraX;
                int drawY = tile.getY() - cameraY;

                g2.drawImage(tile.getImage(), drawX, drawY, this); 
            }

            // --- Draw GameObjects ---
            for (GameObject obj : region.getObjects()) {
                int drawX = obj.getX() * tileSize - cameraX;
                int drawY = obj.getY() * tileSize - cameraY;

                if (obj.images != null && obj.images.length > 0) {
                    g2.drawImage(obj.images[obj.currentImageSlot], drawX, drawY, null);
                } else {
                    // Fallback visual for missing image
                    g2.setColor(Color.RED);
                    g2.fillRect(drawX, drawY, obj.getWidth() * tileSize, obj.getHeight() * tileSize);
                }
            }
        }
    }

    public void centerCamera(int x, int y) {
        this.cameraX = x - getWidth() / 2;
        this.cameraY = y - getHeight() / 2;
        repaint();
    }

    public void loadOrCreateRegion(long id) {
        if (!cachedRegions.containsKey(id)) {
            Region region = (Region) XMLPersistence.loadXML(Constants.CACHE_DIR + "/mapdata/" + id + ".xml");
            if (region == null) {
                int x = (int) (id >> 32);
                int y = (int) id;
                region = new Region(x, y);
            }
            cachedRegions.put(id, region);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if ((e.isControlDown() && e.getKeyCode() == KeyEvent.VK_Z)) {
            undo();
        } else if ((e.isControlDown() && e.getKeyCode() == KeyEvent.VK_Y)) {
            redo();
        }
        if (previewObject != null && placingObject) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT ->
                    previewObject.setX(previewObject.getX() - 1);
                case KeyEvent.VK_RIGHT ->
                    previewObject.setX(previewObject.getX() + 1);
                case KeyEvent.VK_UP ->
                    previewObject.setY(previewObject.getY() - 1);
                case KeyEvent.VK_DOWN ->
                    previewObject.setY(previewObject.getY() + 1);
                case KeyEvent.VK_ENTER ->
                    finalizePlacement();
            }
            repaint();
            return;
        }
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W, KeyEvent.VK_UP ->
                up = true;
            case KeyEvent.VK_S, KeyEvent.VK_DOWN ->
                down = true;
            case KeyEvent.VK_A, KeyEvent.VK_LEFT ->
                left = true;
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT ->
                right = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W, KeyEvent.VK_UP ->
                up = false;
            case KeyEvent.VK_S, KeyEvent.VK_DOWN ->
                down = false;
            case KeyEvent.VK_A, KeyEvent.VK_LEFT ->
                left = false;
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT ->
                right = false;
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        int worldX = e.getX() + cameraX;
        int worldY = e.getY() + cameraY;
        hoverX = worldX / tileSize;
        hoverY = worldY / tileSize;
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (previewObject != null && !placingObject && SwingUtilities.isLeftMouseButton(e)) {
            previewObject.setX(hoverX);
            previewObject.setY(hoverY);
            placingObject = true;
            repaint();
            return;
        }
        int worldX = e.getX() + cameraX;
        int worldY = e.getY() + cameraY;
        int tx = worldX / tileSize;
        int ty = worldY / tileSize;

        long tileId = Utilities.intsToLong(tx, ty);
        if (SwingUtilities.isLeftMouseButton(e)) {
            if (!selectedTiles.containsKey(tileId)) {
                Region region = cachedRegions.get(Utilities.intsToLong(tx, ty));
                if (region != null) {
                    Tile tile = region.get(tileId);
                    if (tile != null) {
                        selectedTiles.put(tileId, tile);
                    }
                }
            }
        } else if (SwingUtilities.isRightMouseButton(e)) {
            selectedTiles.remove(tileId);
        }
        repaint();
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

    public void changeSelectedTileType(int newType) {
        HashMap<Long, Integer> previousTypes = new HashMap<>();
        for (Tile tile : selectedTiles.values()) {
            previousTypes.put(tile.getId(), tile.getType());
            tile.setType(newType);
            modifiedRegionIds.add(tile.getRegionId());
        }
        pushHistory(
                () -> {
                    for (Tile tile : selectedTiles.values()) {
                        tile.setType(previousTypes.get(tile.getId()));
                    }
                },
                () -> {
                    for (Tile tile : selectedTiles.values()) {
                        tile.setType(newType);
                    }
                }
        );

        repaint();
    }

    public void finalizePlacement() {
        GameObject placed = previewObject;
        long regionId = placed.getCurrentRegionId();
        Region region = cachedRegions.get(regionId);
        if (region != null) {
            region.addObject(placed);
            modifiedRegionIds.add(regionId);
            pushHistory(
                    () -> region.getObjects().remove(placed),
                    () -> region.addObject(placed)
            );
        }

        placingObject = false;
        previewObject = null;
        repaint();
    }

    public void pushHistory(Runnable undoAction, Runnable redoAction) {
        if (undoStack.size() >= MAX_HISTORY) {
            undoStack.removeFirst();
        }
        undoStack.addLast(undoAction);
        redoStack.clear();
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            Runnable action = undoStack.removeLast();
            action.run();
            redoStack.addLast(action);
            repaint();
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            Runnable action = redoStack.removeLast();
            action.run();
            undoStack.addLast(action);
            repaint();
        }
    }

    public void saveModifiedRegions() {
        for (Long id : modifiedRegionIds) {
            Region region = cachedRegions.get(id);
            if (region != null) {
                XMLPersistence.saveXML(region);
            }
        }
        modifiedRegionIds.clear();
    }

    public void loadRegion(Region region) {
        cachedRegions.put(region.getId(), region);
    }

    public Collection<Tile> getSelectedTiles() {
        return selectedTiles.values();
    }

    public void setObjectPreview(File file) {
        try {
            GameObject obj = (GameObject) XMLPersistence.loadObject(file);
            if (obj != null) {
                obj.loadImages();
                previewObject = obj;
                placingObject = false;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
