package org.atlis.mapeditorold.add;

import org.atlis.common.tsk.Task;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.atlis.common.model.GameObject;
import org.atlis.common.tsk.TaskPool;
import org.atlis.common.util.Utilities;
import org.atlis.mapeditorold.MapEditor;
import org.atlis.mapeditorold.Screen;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class AddObject extends JFrame {

    private final DefaultListModel<File> objectListModel = new DefaultListModel<>();
    private final JList<File> objectList = new JList<>(objectListModel);
    private final JTextField searchField = new JTextField();
    private final JPanel previewPanel = new JPanel() {
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (previewObject != null
                    && previewObject.images != null
                    && previewObject.images.length > 0
                    && previewObject.images[previewObject.currentImageSlot] != null) {
                g.drawImage(previewObject.images[previewObject.currentImageSlot], 10, 10, null);
            }
        }
    };
    private final JButton selectButton = new JButton("Select Object");

    private GameObject previewObject;
    private Task animationTask;
    private java.util.List<File> allObjects = new ArrayList<>();

    public AddObject() {
        setTitle("Add Object to Map");
        setSize(600, 400);
        Utilities.centerWindow(this);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JScrollPane scrollPane = new JScrollPane(objectList);
        previewPanel.setPreferredSize(new Dimension(300, 300));

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(previewPanel, BorderLayout.CENTER);
        rightPanel.add(selectButton, BorderLayout.SOUTH);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(searchField, BorderLayout.NORTH);
        leftPanel.add(scrollPane, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        add(splitPane, BorderLayout.CENTER);

        objectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        objectList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value.getName());
            if (isSelected) {
                label.setOpaque(true);
                label.setBackground(Color.LIGHT_GRAY);
            }
            return label;
        });

        objectList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                SwingUtilities.invokeLater(this::loadPreviewFromSelected);
            }
        });

        objectList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    selectCurrentObject();
                }
            }
        });

        objectList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    selectCurrentObject();
                }
            }
        });

        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filterObjects(searchField.getText());
            }
        });

        selectButton.addActionListener(e -> selectCurrentObject());

        loadAvailableObjects();
    }

    private void filterObjects(String filter) {
        objectListModel.clear();
        java.util.List<File> filtered = allObjects.stream()
                .filter(f -> f.getName().toLowerCase().contains(filter.toLowerCase()))
                .collect(Collectors.toList());

        for (File file : filtered) {
            objectListModel.addElement(file);
        }

        if (!filtered.isEmpty()) {
            objectList.setSelectedIndex(0);
        } else {
            previewObject = null;
            previewPanel.repaint();
        }
    }

    private void loadPreviewFromSelected() {
        File selected = objectList.getSelectedValue();
        if (selected != null) {
            try {
                // Parse XML
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(selected);
                doc.getDocumentElement().normalize();

                Element objectElement = doc.getDocumentElement();
                int x = Integer.parseInt(objectElement.getAttribute("x"));
                int y = Integer.parseInt(objectElement.getAttribute("y"));
                int width = Integer.parseInt(objectElement.getAttribute("width"));
                int height = Integer.parseInt(objectElement.getAttribute("height"));
                boolean animated = Boolean.parseBoolean(objectElement.getAttribute("animated"));

                boolean loop = false;
                int frameDuration = 0;
                if (objectElement.hasAttribute("loop")) {
                    loop = Boolean.parseBoolean(objectElement.getAttribute("loop"));
                }
                if (objectElement.hasAttribute("frameDuration")) {
                    frameDuration = Integer.parseInt(objectElement.getAttribute("frameDuration"));
                }

                String[] dirs = new String[0];
                if (objectElement.hasAttribute("dirs")) {
                    dirs = objectElement.getAttribute("dirs").split(",");
                }

                // Build GameObject
                previewObject = new GameObject(x, y, width, height, animated, dirs);
                previewObject.loop = loop;
                previewObject.frameDuration = frameDuration;
                previewObject.loadImages();
                previewObject.currentImageSlot = 0;
                previewPanel.repaint();
                startAnimation();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void selectCurrentObject() {
        if (previewObject != null) {
            Screen.objectToAdd = previewObject;
            Screen.addingObject = true;
            if (animationTask != null) {
                TaskPool.remove(animationTask);
            }
            dispose();
        }
    }

    private void loadAvailableObjects() {
        File dir = new File("./cache/mapdata/objects/");
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".xml"));
            if (files != null) {
                for (File file : files) {
                    objectListModel.addElement(file);
                    allObjects.add(file);
                }
                objectList.setSelectedIndex(0);
            }
        }
    }

    private void startAnimation() {
        if (animationTask != null) {
            TaskPool.remove(animationTask);
        }

        if (previewObject != null && previewObject.images != null && previewObject.images.length > 1) {
            animationTask = new Task(150) {
                @Override
                public void execute() {
                    previewObject.currentImageSlot = (previewObject.currentImageSlot + 1) % previewObject.images.length;
                    previewPanel.repaint();
                }
            };
            TaskPool.add(animationTask);
        }
    }

    public static void open() {
        SwingUtilities.invokeLater(() -> new AddObject().setVisible(true));
    }
}
