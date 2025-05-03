/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.atlis.client.ui;

import java.awt.Graphics2D;

/**
 *
 * @author smokey
 */
public abstract class Component {
    
    public int x, y, width, height;
    
    public Component(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public abstract void paint(Graphics2D g);

    public boolean contains(int px, int py) {
        return px >= x && py >= y && px <= x + width && py <= y + height;
    }
}
