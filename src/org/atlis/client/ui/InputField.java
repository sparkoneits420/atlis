package org.atlis.client.ui;

import java.awt.*;
import java.awt.event.*;

public class InputField extends Component implements MouseListener, KeyListener {

    public StringBuilder text = new StringBuilder();
    public boolean focused, showCursor, passwordField;
    public long lastBlink = System.currentTimeMillis();
    public int blinkRate = 500; // milliseconds 
    public static final String STARS = "*******************";
    public static final int MAX_LENGTH = 16;

    public InputField(int x, int y, int width, int height) {
        super(x, y, width, height);
    } 
     
    @Override
    public void paint(Graphics2D g) {
        // Optional: Call this to simulate blinking (externally)
        updateBlinkState();

        // Draw the text
        g.setColor(Color.WHITE);
        g.setFont(new Font("Calibri", Font.PLAIN, 16));
        FontMetrics fm = g.getFontMetrics();
        int textY = y + ((height - fm.getHeight()) / 2) + fm.getAscent();
        if (!passwordField) {
            g.drawString(text.toString(), x + 5, textY);
        } else {
            g.drawString(STARS.substring(0, text.length()), x + 5, textY);
        } 
        
        // Draw the cursor
        if (focused && showCursor) {
            int cursorX = x + 5 + fm.stringWidth((passwordField ? STARS.substring(0, text.length()) : text.toString())); 
            g.drawLine(cursorX, y, cursorX, y + height - 10);
        }
    }

    public void updateBlinkState() {
        long now = System.currentTimeMillis();
        if (now - lastBlink >= blinkRate) {
            showCursor = !showCursor;
            lastBlink = now;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if (!focused) {
            return;
        }

        char c = e.getKeyChar();
        if (Character.isISOControl(c)) {
            if (c == '\b' && text.length() > 0) {
                text.deleteCharAt(text.length() - 1);
            }
        } else if(text.length() <= MAX_LENGTH) {
            text.append(c);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // Not used
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // Not used
    }

    public void clearText() {
        text.setLength(0);
    }

    public String getText() {
        return text.toString();
    }

    public void setText(String t) {
        text = new StringBuilder(t);
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    public boolean isFocused() {
        return focused;
    }

    public boolean withinBoundry(int x2, int y2) {   
        boolean b = x2 >= x && y2 >= y && x2 <= x + width && y2 <= y + height; 
        return b;
    } 
    
    @Override
    public void mouseClicked(MouseEvent e) {
        focused = withinBoundry(e.getX(), e.getY() - 24);
    }

    @Override
    public void mousePressed(MouseEvent e) { }

    @Override
    public void mouseReleased(MouseEvent e) { }

    @Override
    public void mouseEntered(MouseEvent e) { }

    @Override
    public void mouseExited(MouseEvent e) { }
}
