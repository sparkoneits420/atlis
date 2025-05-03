package org.atlis.client.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.event.EventListenerList;
import org.atlis.client.Client;

public abstract class Button extends Component implements KeyListener, MouseListener { 
    
    public boolean pressed = false;
    public boolean hover = false;

    public BufferedImage normalImage;
    public BufferedImage hoverImage;
    public BufferedImage pressedImage;
    public String label;
    public final EventListenerList listenerList = new EventListenerList();

    public Button(int x, int y, int width, int height,
                  BufferedImage normalImage,
                  BufferedImage hoverImage,
                  BufferedImage pressedImage, String label) {
        super(x, y, width, height);
        this.normalImage = normalImage;
        this.hoverImage = hoverImage;
        this.pressedImage = pressedImage; 
        this.label = label;
    }

    @Override
    public void paint(Graphics2D g) {
        
        Point p = Client.getClient().getMousePosition();
        if(p != null) 
            hover = withinBounds(p.x, p.y);
        BufferedImage img = normalImage;
        if (pressed && pressedImage != null) {
            img = pressedImage;
        } else if (hover && hoverImage != null) {
            img = hoverImage;
        }

        if (img != null) {
            g.drawImage(img, x, y, width, height, null);
        } else {
            g.setColor(Color.GRAY);
            g.fillRect(x, y, width, height);
        }
        g.setColor(Color.WHITE);
        g.setFont(new Font("Calibri", Font.BOLD, 20));
        g.drawString(label, Client.screen.getWidth() / 2 - g.getFontMetrics().stringWidth(label) / 2, 
                Client.screen.getHeight() / 2 - (Client.screen.loginScreen.LOGIN_INTERFACE.getHeight() / 6) + 230);
    }

    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }

    public void fireActionEvent() {
        ActionEvent evt = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "buttonClicked");
        actionPerformed(evt);
    }

    public boolean withinBounds(int mx, int my) {
        return mx >= x + 7 && my >= y + 30 && mx <= x + width + 7 && my <= y + height + 30;
    }

    @Override
    public void mouseClicked(MouseEvent e) { 
        if (withinBounds(e.getX(), e.getY())) {
            fireActionEvent();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        pressed = withinBounds(e.getX(), e.getY());
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (pressed && withinBounds(e.getX(), e.getY())) {
            fireActionEvent();
        }
        pressed = false;
    }

    @Override
    public void mouseEntered(MouseEvent e) { 
    }

    @Override
    public void mouseExited(MouseEvent e) {
        hover = false;
        pressed = false;
    }
 
    public abstract void actionPerformed(ActionEvent e);

    @Override
    public void keyTyped(KeyEvent e) { }

    @Override
    public void keyPressed(KeyEvent e) { }

    @Override
    public void keyReleased(KeyEvent e) { }
}