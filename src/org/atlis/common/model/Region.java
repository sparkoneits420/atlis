package org.atlis.common.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import org.atlis.common.util.Constants;
import org.atlis.common.util.Log;
import org.atlis.common.util.Utilities;

public class Region extends HashMap<Long, Tile> {

    private long id;
    public ArrayList<GameObject> objects;
    public ArrayList<NPC> npcs;

    public Region(int x, int y) {
        this.id = ((long) Math.floorDiv(x, Constants.REGION_SIZE) << 32)
                | (Math.floorDiv(y, Constants.REGION_SIZE) & 0xFFFFFFFFL);
        for (int j = (-(Constants.REGION_SIZE / 2));
                j < Constants.REGION_SIZE / 2; j += 16) {
            for (int k = (-(Constants.REGION_SIZE / 2));
                    k < Constants.REGION_SIZE / 2; k += 16) {
                long tileId = Utilities.intsToLong(x + j, y + k);
                Tile tile = new Tile(x + j, y + k, Tile.GRASS, tileId);
                if (!this.containsValue(tile)) {
                    put(tileId, tile);
                }
            }
        }
        objects = new ArrayList<>();
        npcs = new ArrayList<>();
    }

    public Region(long id) {
        this.id = id;
        int x = getX();
        int y = getY();
        for (int j = (-(Constants.REGION_SIZE / 2));
                j < Constants.REGION_SIZE / 2; j += 16) {
            for (int k = (-(Constants.REGION_SIZE / 2));
                    k < Constants.REGION_SIZE / 2; k += 16) {
                long tileId = Utilities.intsToLong(x + j, y + k);
                Tile tile = new Tile(x + j, y + k, Tile.GRASS, tileId);
                if (!this.containsValue(tile)) {
                    put(tileId, tile);
                }
            }
        }
        objects = new ArrayList<>();
        npcs = new ArrayList<>();
    }

    public boolean isTileBlocked(int x, int y, int w, int h) {
        Log.print("[CLIP] Region has " + objects.size() + " objects");
        for (GameObject obj : objects) {
            if (!obj.isBlocking()) {
                continue;
            }

            if (x + w > obj.x && x < obj.x + obj.getWidth()
                    && y + h > obj.y && y < obj.y + obj.getHeight()) {
                return true;
            }
        }
        return false;
    }

    public long getId() {
        return id;
    }

    public final int getX() {
        return (int) (id >> 32);
    }

    public final int getY() {
        return (int) (id & 0xFFFFFFFFL);
    }

    public ArrayList<GameObject> getObjects() {
        return objects;
    }

    public ArrayList<NPC> getNPCs() {
        return npcs;
    }

    public boolean isVisible(Region center) {
        int dx = Math.abs(center.getX() - getX());
        int dy = Math.abs(center.getY() - getY());
        return dx <= 1 && dy <= 1;
    }

    public void addObject(GameObject gameObject) {
        objects.add(gameObject);
    }
}
