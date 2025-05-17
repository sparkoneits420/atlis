// MapEditor.java - with Save menu item and Ctrl+S shortcut
package org.atlis.mapeditor;

import java.io.File;

import org.atlis.common.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MapEditor extends JFrame implements KeyListener, MouseListener, MouseMotionListener {

    public Screen canvas = new Screen();

    public final DefaultListModel<ImageIcon> iconModel = new DefaultListModel<>();
    public final JList<ImageIcon> entityList = new JList<>(iconModel);
    public final JToggleButton selectButton = new JToggleButton("Select");
    public final JToggleButton pathButton = new JToggleButton("Pathway");
    public final JToggleButton objectButton = new JToggleButton("Object");
    public final JToggleButton npcButton = new JToggleButton("NPC");

    public MapEditor() {
        super("Atlis Map Editor");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setSize(Constants.DEFAULT_SCREEN_WIDTH, Constants.DEFAULT_SCREEN_HEIGHT);
        add(canvas, BorderLayout.CENTER);

        // Sidebar with tile icon list
        entityList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        entityList.setLayoutOrientation(JList.VERTICAL);
        entityList.setVisibleRowCount(-1);
        entityList.setCellRenderer(new DefaultListCellRenderer() {
        
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setIcon((ImageIcon) value);
                label.setText("");
                label.setHorizontalAlignment(SwingConstants.CENTER);
                return label;
            }
        });  
        JScrollPane scrollPane = new JScrollPane(entityList);
        scrollPane.setPreferredSize(new Dimension(160, getHeight()));
        add(scrollPane, BorderLayout.EAST);

        // Determine mode based on toggle selection
        iconModel.clear();
        boolean editingTiles = selectButton.isSelected() || !canvas.getSelectedTiles().isEmpty();
        if (editingTiles) {
            var tileImages = XMLPersistence.getTileImages();
            if (tileImages != null) {
                tileImages.values().forEach(img -> iconModel.addElement(new ImageIcon(img)));
            }
        } else {
            File objectDir = new File(Constants.CACHE_DIR + "/objects");
            if (objectDir.exists() && objectDir.isDirectory()) {
                File[] files = objectDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        iconModel.addElement(new ImageIcon(f.getAbsolutePath()));
                    }
                }
            }
        }

        entityList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && objectButton.isSelected()) {
                int index = entityList.getSelectedIndex();
                if (index >= 0) {
                    File objectDir = new File(Constants.CACHE_DIR + "/objects");
                    File[] files = objectDir.listFiles();
                    if (files != null && index < files.length) {
                        File selected = files[index];
                        canvas.setObjectPreview(selected);
                    }
                }
            }
        });

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(new JMenuItem("New"));
        fileMenu.add(new JMenuItem("Open"));

        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        saveItem.addActionListener(e -> canvas.saveModifiedRegions());
        fileMenu.add(saveItem);

        fileMenu.addSeparator();
        fileMenu.add(new JMenuItem("Exit"));
        menuBar.add(fileMenu);

        JMenu editMenu = new JMenu("Edit");
        editMenu.add(new JMenuItem("Paint Terrain"));
        editMenu.add(new JMenuItem("Place Object"));
        editMenu.add(new JMenuItem("Place NPC"));
        editMenu.add(new JMenuItem("Erase"));
        menuBar.add(editMenu);

        setJMenuBar(menuBar);

        JToolBar toolbar = new JToolBar();
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(selectButton);
        toolbar.add(selectButton);
        modeGroup.add(pathButton);
        toolbar.add(pathButton);
        modeGroup.add(objectButton);
        toolbar.add(objectButton);
        modeGroup.add(npcButton);
        toolbar.add(npcButton);

        add(toolbar, BorderLayout.NORTH);

        addKeyListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
 
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!canvas.modifiedRegionIds.isEmpty()) {
                    int result = JOptionPane.showConfirmDialog(
                            MapEditor.this,
                            "You have unsaved changes. Save before exiting?",
                            "Unsaved Changes",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE
                    );
                    if (result == JOptionPane.CANCEL_OPTION) {
                        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
                    } else if (result == JOptionPane.YES_OPTION) {
                        canvas.saveModifiedRegions();
                        setDefaultCloseOperation(EXIT_ON_CLOSE);
                    } else {
                        setDefaultCloseOperation(EXIT_ON_CLOSE);
                    }
                }
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MapEditor().setVisible(true);
        });
    }

    @Override public void keyTyped(KeyEvent e) {
        canvas.keyTyped(e);
    }
    @Override public void keyPressed(KeyEvent e) {
        //handle hotkey
        canvas.keyPressed(e);
    }
    @Override public void keyReleased(KeyEvent e) {
        canvas.keyReleased(e);
    }
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void mouseDragged(MouseEvent e) {}
    @Override public void mouseMoved(MouseEvent e) {}
}
