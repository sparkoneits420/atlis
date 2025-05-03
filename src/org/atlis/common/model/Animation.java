package org.atlis.common.model;
 
import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.Serializable; 
import org.atlis.client.Client;
import org.atlis.common.util.Utilities;

public class Animation implements Serializable {
    
    public static final long serialVersionUID = 232L;

    public transient int currentIndex = 0;
    public transient Image[] images;
    public String[] dirs;

    public Animation(String dir, int size) throws IOException {
        dirs = new String[size];
        images = new Image[size];
        for (int i = 0; i < size; i++) { 
            Image image = Utilities.filterRGBA(ImageIO.read(new File(dir + i + ".png")));
            dirs[i] = dir + i + ".png";
            if (image != null) {
                images[i] = image;
            }
        }
    }

    public Animation(String[] dirs, int size) throws IOException {
        dirs = new String[size];
        images = new Image[size];
        for (int i = 0; i < size; i++) {
            Image image = ImageIO.read(new File(dirs[i]));
            if (image != null) {
                images[i] = image;
            }
        }
    }
    
    public void repopulate() {
        images = new Image[dirs.length];
        for (int i = 0; i < dirs.length; i++) { 
            try { 
                Image image = Utilities.filterRGBA(ImageIO.read(new File(dirs[i])));
                if (image != null) {
                    images[i] = image;
                }
            }catch (IOException ex) {
                Client.getLog().put(ex.getMessage());
            }
        }
    }
}
