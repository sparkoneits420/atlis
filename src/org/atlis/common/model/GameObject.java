package org.atlis.common.model; 

import java.awt.Image;
import java.io.File;
import java.io.IOException; 
import java.io.Serializable; 
import javax.imageio.ImageIO; 
import org.atlis.common.util.Utilities; 

public class GameObject extends Entity {
 
    public transient Image[] images; 
    public String[] dirs;
    public int currentImageSlot;
     
    public boolean loop, boundry;
    public int frameDuration;

    public GameObject(int x, int y) {
        super();
        this.id = (((long) x << 32) | (y & 0xffffffffL));
        this.x = x;
        this.y = y;
    }

    public GameObject(int x, int y, int width, int height, boolean animated, String[] dirs) {
        super();
        this.id = (((long) x << 32) | (y & 0xffffffffL));
        this.bounds = new int[4]; 
        this.animated = animated; 
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height; 
        this.dirs = dirs; 
    } 
 
    public void loadImages() {
        images = new Image[dirs.length];
        for(int i = 0; i < dirs.length; i++) {
            try { 
                images[i] = Utilities.filterRGBA(ImageIO.read(new File(dirs[i])));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public boolean isBoundry() {
        return boundry;
    }

    public void setBoundry(boolean boundry) {
        this.boundry = boundry;
    }
}
