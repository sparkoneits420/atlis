package org.atlis.common.model;

import java.awt.Image;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.atlis.common.util.Constants;
import org.atlis.common.util.Log;
import org.atlis.common.util.Utilities;
import org.atlis.common.util.XMLPersistence;
import org.atlis.server.Server;

public class Player extends Entity {

    public static final long serialVersionUID = 232L;

    public String username, password;
    public boolean moved, moving;
    public Animation[] walkAnim;
    public Animation current;
    public boolean running;
    public transient Image idle;
    public int animationCycle;
    public int walkDirection;
    public long regionRequest;
    public boolean collision = false;
    public HashMap<Long, Region> currentRegions;
    public boolean regionRequested = false;
    public int lastRegionX;
    public int lastRegionY;

    public Player() {
        super();
        //this.region = (Region) XMLPersistence.load(Constants.CACHE_DIR + "/mapdata/" + regionId + ".map");
        this.updateFlags = new ConcurrentLinkedQueue<>();
        this.currentRegions = new HashMap<>();
        try {
            walkAnim = new Animation[24];
            //TODO: load this as an update packet under appearance update flag
            walkAnim[0] = new Animation(Constants.CACHE_DIR + "/player/nate/NATEFW", 3);
            walkAnim[1] = new Animation(Constants.CACHE_DIR + "/player/nate/NATEBK", 3);
            walkAnim[2] = new Animation(Constants.CACHE_DIR + "/player/nate/NATELF", 3);
            walkAnim[3] = new Animation(Constants.CACHE_DIR + "/player/nate/NATERT", 3);
            idle = walkAnim[1].images[0];
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    } 

    public List<Long> getVisibleRegions() {
        List<Long> visible = new ArrayList<>();

        int regionSize = Constants.REGION_SIZE;

        // Find the base region the player is inside (grid coordinates)
        int playerRegionX = Math.floorDiv(x, regionSize);
        int playerRegionY = Math.floorDiv(y, regionSize);
 
        int loadDistance = 1;

        for (int dx = -loadDistance; dx <= loadDistance; dx++) {
            for (int dy = -loadDistance; dy <= loadDistance; dy++) {
                int regionX = (playerRegionX + dx) * regionSize; // back to pixel coords
                int regionY = (playerRegionY + dy) * regionSize; // back to pixel coords
                //Log.print(regionX + ", " + regionY);
                visible.add(Utilities.intsToLong(regionX, regionY));
            }
        }

        return visible;
    }

    public boolean requiresRegionalUpdate() {
        int size = Constants.REGION_SIZE;
        int currentRegionX = (x / size) * size;
        int currentRegionY = (y / size) * size;

        if (region == null) {
            if (!regionRequested) {
                regionRequested = true;
                lastRegionX = currentRegionX;
                lastRegionY = currentRegionY;
                return true;
            }
            return false;
        }

        if (currentRegionX != lastRegionX || currentRegionY != lastRegionY) {
            lastRegionX = currentRegionX;
            lastRegionY = currentRegionY;
            return true;
        }

        return false;
    }

    public boolean withinRegion(long regionId) {
        int regionSize = Constants.REGION_SIZE;

        int regionX = (int) (regionId >> 32);
        int regionY = (int) (regionId & 0xFFFFFFFFL);

        return x >= regionX && x < regionX + regionSize
                && y >= regionY && y < regionY + regionSize;
    }

    public HashMap<Long, Region> getCurrentRegions() {
        return currentRegions;
    }

    public void setRegionRequest(long regionId) {
        this.regionRequest = regionId;
    }

    public int getWalkDirection() {
        return walkDirection;
    }

    public void setWalkDirection(int walkDirection) {
        this.walkDirection = walkDirection;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public long getRegionRequest() {
        return regionRequest;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    } 
} 