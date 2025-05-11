package org.atlis.client;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import org.atlis.common.tsk.TaskPool;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import org.atlis.client.ui.ChatInterface;
import org.atlis.client.ui.Component;
import org.atlis.common.util.Constants;
import org.atlis.common.util.Log;
import org.atlis.common.util.Utilities;

public class Client extends JFrame {

    public static ExecutorService executor; 
    public static Screen screen; 
    public static Client client;

    public Client(boolean extended) {
        super(Constants.GAME_TITLE);
        executor = Executors.newCachedThreadPool();  
        TaskPool.start();
        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // If Nimbus is not available, you can set the GUI to another look and feel.
        } 
        if (!extended) {
            initComponents();
        }
    }

    private void initComponents() {
        screen = new Screen();
        addKeyListener(screen.keyManager);
        addMouseListener(screen.mouseManager);
        addMouseMotionListener(screen.mouseManager);
        setIconImage(Toolkit.getDefaultToolkit().getImage(Constants.CACHE_DIR + "/interface/atlis_icon.png"));
        for (Component c : screen.loginScreen.components) {
            addMouseListener((MouseListener) c);
            addKeyListener((KeyListener) c);
        }
        this.setPreferredSize(new Dimension(Constants.DEFAULT_SCREEN_WIDTH,
                Constants.DEFAULT_SCREEN_HEIGHT));
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (JOptionPane.showConfirmDialog(screen,
                        "Are you sure you want to exit?",
                        "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            }
        });
        add(screen);
        pack();
        Utilities.centerWindow(this);
        addListeners();
    }

    public static void main(String[] args) {
        loadProperties(".properties");
        Client.getClient().setVisible(true);
    }

    public static void loadProperties(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            Properties props = new Properties();
            props.load(fis);
            for (String name : props.stringPropertyNames()) {
                System.setProperty(name, props.getProperty(name));
            }
        } catch (IOException e) {
            System.err.println("Failed to load properties: " + e.getMessage());
        }
    }
    
    public void addListeners() {
        ChatInterface chat = screen.chat;
        addMouseListener(chat);
        addKeyListener(chat);
        addMouseWheelListener(chat);
        
    } 

    public static Screen getScreen() {
        return screen;
    }

    public static Client getClient() {
        if (client == null) {
            client = new Client(false);
        }
        return client;
    }

    public static ExecutorService getThreadPool() {
        return executor;
    }
 
}
