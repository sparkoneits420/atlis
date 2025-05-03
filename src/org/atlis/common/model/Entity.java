package org.atlis.common.model;

import java.util.Queue;

public class Entity {

    public int x, y, z, width, height;
    public long regionId;
    public Region region;
    public long id;
    public boolean animated, blocking = true;
    public Queue<UpdateFlag> updateFlags;
    public int[] bounds; 

    public Entity() {
        setBounds();
    }

    public boolean canMoveDirection(int direction) {
        int nextX = this.x;
        int nextY = this.y;

        switch (direction) {
            case 0 -> nextY -= 1; 
            case 1 -> nextX += 1; 
            case 2 -> nextY += 1; 
            case 3 -> nextX -= 1; 
            default -> {
                return false; // invalid direction
            }
        }

        if (region == null) {
            return false;
        }

        return !region.isTileBlocked(nextX, nextY, this.getWidth(), this.getHeight());
    }

    public boolean isBlocking() {
        return blocking;
    }
    
    public void setBound(boolean blocking) {
        this.blocking = blocking;
    }
    
    public long getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public long getRegionId() {
        return regionId;
    }

    public void setRegionId(long regionId) {
        this.regionId = regionId;
    }

    public void setLocation(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public Region getRegion() {
        return region;
    }

    public Queue<UpdateFlag> getUpdateFlags() {
        return updateFlags;
    }

    public void addUpdateFlag(UpdateFlag flag) {
        if (!updateFlags.contains(flag)) {
            updateFlags.add(flag);
        }
    }

    public boolean hasUpdateFlag(UpdateFlag flag) {
        return updateFlags.contains(flag);
    }

    public UpdateFlag pollUpdateFlags() {
        return updateFlags.poll();
    }

    public void clearUpdateFlags() {
        updateFlags.clear();
    }

    public boolean isAnimated() {
        return animated;
    }

    public final void setBounds() {
        this.bounds = new int[4];
        this.bounds[0] = getX();
        this.bounds[1] = getY();
        this.bounds[2] = getX() + width;
        this.bounds[3] = getY() + height;
    }

    public boolean withinBounds(Entity entity) {
        int w = entity.getWidth(), h = entity.getHeight();
        return entity.x - w >= bounds[0] && entity.y - h >= bounds[1]
                && entity.x + w <= bounds[2] && entity.y + h <= bounds[3];
    }

}
