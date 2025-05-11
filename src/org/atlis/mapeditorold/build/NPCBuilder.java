package org.atlis.mapeditorold.build;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.HashMap; 
import org.atlis.common.model.Animation;
import org.atlis.common.model.NPC;
import org.atlis.common.util.Constants;
import org.atlis.common.util.Utilities;

public class NPCBuilder extends JFrame {

    private HashMap<String, Image[]> frames = new HashMap<>();
    private String currentDirection = "south";
    private int currentFrame = 0;
    private JPanel previewPanel;
    private Timer animationTimer;
    private HashMap<Integer, Animation> walkAnim = new HashMap<>();
    
    
    public NPCBuilder() {
        setTitle("NPC Builder");
        setSize(700, 500);
        setLayout(new BorderLayout());
        Utilities.centerWindow(this);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel controlPanel = new JPanel(new GridLayout(5, 2));

        String[] directions = {"north", "south", "east", "west"};
        for (String dir : directions) {
            JButton button = new JButton("Add Frames " + dir);
            button.addActionListener(e -> addFrames(dir));
            controlPanel.add(button);
        }

        JButton saveButton = new JButton("Save NPC");
        saveButton.addActionListener(e -> saveNPC());
        controlPanel.add(saveButton);

        previewPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Image[] directionFrames = frames.get(currentDirection);
                if (directionFrames != null && directionFrames.length > 0) {
                    g.drawImage(directionFrames[currentFrame % directionFrames.length], 50, 50, null);
                }
            }
        };

        add(previewPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        setupAnimation();
        setupKeyBindings();
    }

    private void addFrames(String direction) {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(Constants.CACHE_DIR + "/npcs/"));
        chooser.setMultiSelectionEnabled(true);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = chooser.getSelectedFiles();
            Image[] imgs = new Image[selectedFiles.length];
            try {
                for (int i = 0; i < selectedFiles.length; i++) {
                    imgs[i] = Utilities.filterRGBA(ImageIO.read(selectedFiles[i]));
                }
                frames.put(direction, imgs);
                String st = selectedFiles[0].toString();
                
                Integer index = Constants.DIRECTION_MAP.get(direction);
                if (index != null) {
                    walkAnim.put(index, new Animation(st.substring(0, st.length() - 5), imgs.length));
                }

                System.out.println(direction + ", " + st.substring(0, st.length() - 5));
                repaint();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error loading images.");
            }
        }
    }

    private void setupAnimation() {
        animationTimer = new Timer(200, e -> {
            currentFrame++;
            previewPanel.repaint();
        });
        animationTimer.start();
    }

    private void setupKeyBindings() {
        InputMap im = previewPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = previewPanel.getActionMap();

        im.put(KeyStroke.getKeyStroke("W"), "north");
        im.put(KeyStroke.getKeyStroke("UP"), "north");
        im.put(KeyStroke.getKeyStroke("S"), "south");
        im.put(KeyStroke.getKeyStroke("DOWN"), "south");
        im.put(KeyStroke.getKeyStroke("A"), "west");
        im.put(KeyStroke.getKeyStroke("LEFT"), "west");
        im.put(KeyStroke.getKeyStroke("D"), "east");
        im.put(KeyStroke.getKeyStroke("RIGHT"), "east");

        for (String dir : new String[]{"north", "south", "east", "west"}) {
            am.put(dir, new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    currentDirection = dir;
                    currentFrame = 0;
                    repaint();
                }
            });
        }
    }

    //    public NPC(int x, int y, int width, int height, boolean animated, String[] dirs) {
    private void saveNPC() {
        int walkRadius = Integer.parseInt(JOptionPane.showInputDialog(this, "NPC walk radius:"));
        String name = JOptionPane.showInputDialog(this, "NPC name:");
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(chooser.getSelectedFile()))) {
                NPC npc = new NPC(name, true, walkAnim, walkRadius);
                oos.writeObject(npc);
                oos.close();
                JOptionPane.showMessageDialog(this, "NPC saved successfully.");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error saving NPC.");
                e.printStackTrace();
            }
        }
    }

    public static void open() {
        SwingUtilities.invokeLater(() -> new NPCBuilder().setVisible(true));
    }
}
