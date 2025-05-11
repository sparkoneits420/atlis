package org.atlis.common.model; 
 
import java.awt.*; 
import org.atlis.common.util.XMLPersistence;

public class Tile extends Entity { 

    public int type;
    public boolean walkable = true;
    public boolean isDoor = false; 
    public static final int GRASS = 0;
    public static final int STONE = 1;

    public Tile(int x, int y, long regionId) {
        this.x = x;
        this.y = y;
        this.z = 0;
        this.type = GRASS;
        this.regionId = regionId;
    }

    public Tile(int x, int y, int type, long regionId) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.regionId = regionId;
    } 

    public Tile(int x, int y, int type, long regionId, boolean walkable, boolean isDoor) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.regionId = regionId;
        this.walkable = walkable;
        this.isDoor = isDoor;
    }

    public Image getImage() {
        return XMLPersistence.getTileImages().get(type);
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    } 

    public boolean isVisible(int x, int y, Component screen) {
        return this.x - x <= screen.getWidth() / 1.6
                || this.y - y <= screen.getHeight() / 1.6;
        
    }
}