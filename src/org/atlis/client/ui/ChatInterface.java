package org.atlis.client.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import org.atlis.client.Client;
import org.atlis.common.util.Log;

public class ChatInterface extends Component implements KeyListener, MouseListener, MouseWheelListener {

    private final List<String> messages = new ArrayList<>();
    private final StringBuilder currentInput = new StringBuilder();

    private boolean focused = false;
    private boolean cursorVisible = true;
    private long lastBlink = System.currentTimeMillis();

    private int scrollOffset = 0;
    private static final int MAX_VISIBLE_MESSAGES = 10;
    private static final int CHAR_LIMIT = 250;
    private static final int INPUT_HEIGHT = 30;

    public ChatInterface() {
        super(20, 0, 600, 300);
    }

    public void resizeToWindow(int windowWidth, int windowHeight) {
        this.width = windowWidth / 4;
        this.height = windowHeight / 7;
        this.x = 20;
        this.y = windowHeight - height - 20;
    }

    @Override
    public void paint(Graphics2D g) {
        if (System.currentTimeMillis() - lastBlink > 500) {
            cursorVisible = !cursorVisible;
            lastBlink = System.currentTimeMillis();
        }

        // Draw background
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(x, y, width, height);

        // Divider
        g.setColor(Color.DARK_GRAY);
        g.fillRect(x, y + height - INPUT_HEIGHT, width, 1);

        // Messages
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        int lineY = y + 16;

        int total = messages.size();
        int start = Math.max(0, total - MAX_VISIBLE_MESSAGES - scrollOffset);
        int end = Math.min(total, start + MAX_VISIBLE_MESSAGES);

        for (int i = start; i < end; i++) {
            g.drawString(messages.get(i), x + 5, lineY);
            lineY += 16;
        }

        // Input
        String input = currentInput.toString();
        if (cursorVisible && focused) {
            input += "|";
        }

        g.drawString(input, x + 5, y + height - 10);

        // Scrollbar (only if needed)
        if (messages.size() > MAX_VISIBLE_MESSAGES) {
            int barHeight = (int) ((float) MAX_VISIBLE_MESSAGES / total * (height - INPUT_HEIGHT));
            int barY = y + (int) ((float) scrollOffset / (total - MAX_VISIBLE_MESSAGES) * (height - INPUT_HEIGHT - barHeight));

            g.setColor(new Color(100, 100, 100, 180));
            g.fillRect(x + width - 6, barY, 4, barHeight);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if (!focused) {
            return;
        }
        char c = e.getKeyChar();
        if (Character.isISOControl(c)) {
            return;
        }

        if (currentInput.length() < CHAR_LIMIT) {
            currentInput.append(c);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (!focused) {
            return;
        }

        switch (e.getKeyCode()) {
            case KeyEvent.VK_BACK_SPACE -> {
                if (currentInput.length() > 0) {
                    currentInput.deleteCharAt(currentInput.length() - 1);
                }
            }
            case KeyEvent.VK_ENTER -> {
                if (currentInput.length() > 0) {
                    sendChatMessage(currentInput.toString());
                    currentInput.setLength(0);
                }
            }
            case KeyEvent.VK_UP ->
                scrollOffset = Math.min(scrollOffset + 1, Math.max(0, messages.size() - MAX_VISIBLE_MESSAGES));
            case KeyEvent.VK_DOWN ->
                scrollOffset = Math.max(0, scrollOffset - 1);
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (!contains(e.getX(), e.getY())) {
            return;
        }

        int direction = e.getWheelRotation(); // 1 = down, -1 = up
        scrollOffset += direction;
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, messages.size() - MAX_VISIBLE_MESSAGES)));
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        focused = contains(e.getX(), e.getY());
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    public void addMessage(String message) {
        messages.add(message);
    }

    private void sendChatMessage(String msg) {
        Log.print("Sending chat: " + msg);
        messages.add("You: " + msg);
    }
}
