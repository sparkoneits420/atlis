package org.atlis.editor;

import org.atlis.common.tsk.TaskPool;

import javax.swing.*;
import java.awt.*; 
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException; 
import javax.imageio.ImageIO;
import org.atlis.client.Client; 
import org.atlis.common.util.Constants;
import org.atlis.common.util.Log;
import org.atlis.common.util.Utilities;

public class MapEditor extends Client {
 
    public static Screen render; 

    public MapEditor() {
        super(true); 
        initComponents();
    }

    private void initComponents() {
        render = new Screen();
        JMenuBar menuBar = new JMenuBar();
        String[] menus = new String[]{
            "File", "Edit"
        };
        String[][] menuItems = new String[][]{
            {"New Map", "Open", "Save", "Log Dump", "Exit"},
            {"Replace", "Create Object", "Add Object", "Create NPC", "Add NPC"}
        };

        for (int i = 0; i < menus.length; i++) {
            JMenu menu = new JMenu(menus[i]);
            if (menuItems.length > i) {
                for (String s : menuItems[i]) {
                    JMenuItem item = new JMenuItem(s);
                    item.addActionListener(render);
                    menu.add(item);
                }
            }
            menuBar.add(menu);
        }

        setLayout(new BorderLayout());
        setSize(new Dimension(Constants.DEFAULT_SCREEN_WIDTH, 
                Constants.DEFAULT_SCREEN_HEIGHT));
        Utilities.centerWindow(this);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        addKeyListener(render); 
        SwingUtilities.invokeLater(() -> {
            add(render);
        });
        SwingUtilities.invokeLater(() -> {
            setJMenuBar(menuBar);
        }); 
    }
 
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        String loadingMessage = "Loading...";
        if (!Screen.initialized) { 
            g.setFont(new Font("Serif", Font.BOLD, 32));
            try {
                g.drawImage(ImageIO.read(new File(Constants.CACHE_DIR + "/interface/background.jpg"))
                        .getScaledInstance(getWidth(), getHeight(), Image.SCALE_SMOOTH), 0, 0, this);
            } catch (IOException ex) {
                MapEditor.getLog().put(ex.getMessage());
            }
            g.drawString(loadingMessage, (getWidth() / 2) - (g.getFontMetrics().stringWidth(loadingMessage) / 2), getHeight() / 2);
        } else {
            super.paint(g);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MapEditor().setVisible(true);
        });
    }

    public static TaskPool getTaskPool() {
        return taskPool;
    }

    public static Log getLog() {
        return log;
    }
 
}
