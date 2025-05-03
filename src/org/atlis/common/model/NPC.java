package org.atlis.common.model;
 
import java.awt.Image; 
import java.util.HashMap; 

public class NPC extends Entity { 
 
    public HashMap<Integer, Animation> walkAnim; 
    public int walkRadius;
    public transient Image idle;
    public String name;
    public transient boolean moving = true;
    public transient int walkDirection; 
    public transient Animation currentAnim;
    public transient int animationCycle; 
    public transient int[] boundry;

    public static final long serialVersionUID = 666L;

    public NPC(String name, boolean animated, HashMap<Integer, Animation> walkAnim, int walkRadius) { 
        this.name = name;
        this.id = (int) (Math.random() * Integer.MAX_VALUE); 
        this.animated = animated; 
        this.walkAnim = walkAnim;
        this.walkRadius = walkRadius;
        this.idle = walkAnim.get(1).images[0];
    } 

    public HashMap<Integer, Animation> getWalkAnim() {
        return walkAnim;
    }

    public void setWalkAnim(HashMap<Integer, Animation> walkAnim) {
        this.walkAnim = walkAnim;
    }

    public int[] getBounds() {
        return bounds;
    }

    public void setBounds(int[] bounds) {
        this.bounds = bounds;
    }

    public int getWalkRadius() {
        return walkRadius;
    }

    public void setWalkRadius(int walkRadius) {
        this.walkRadius = walkRadius;
    }

    public Image getIdle() {
        return idle;
    }

    public void setIdle(Image idle) {
        this.idle = idle;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isMoving() {
        return moving;
    }

    public void setMoving(boolean moving) {
        this.moving = moving;
    }

    public int getWalkDirection() {
        return walkDirection;
    }

    public void setWalkDirection(int walkDirection) {
        this.walkDirection = walkDirection;
    }
 
}
