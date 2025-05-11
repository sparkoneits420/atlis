package org.atlis.mapeditorold.build;

import javax.swing.*;
import java.awt.*; 
import java.io.*;
import java.util.ArrayList;
import java.util.List; 
import org.atlis.common.model.GameObject;
import org.atlis.common.util.Constants;
import org.atlis.common.util.Utilities;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.awt.image.BufferedImage;
import java.io.File;

public class ObjectBuilder extends JFrame {

    private DefaultListModel<File> frameListModel = new DefaultListModel<>();
    private JList<File> frameList = new JList<>(frameListModel);
    private JButton addFramesButton = new JButton("Add Frames");
    private JButton saveButton = new JButton("Save Object");
    private JCheckBox loopCheckbox = new JCheckBox("Loop Animation");
    private JSpinner frameDurationSpinner = new JSpinner(new SpinnerNumberModel(100, 10, 10000, 10));
    private JPanel previewPanel = new JPanel() {
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (!previewFrames.isEmpty()) {
                g.drawImage(previewFrames.get(currentFrame), 10, 10, null);
            }
        }
    };

    private Timer previewTimer;
    private List<Image> previewFrames = new ArrayList<>();
    private int currentFrame = 0;

    public ObjectBuilder() {
        setTitle("Animated Object Builder");
        setSize(700, 400);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        Utilities.centerWindow(this);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayout(0, 1));

        controlPanel.add(new JLabel("Frame Duration (ms):"));
        controlPanel.add(frameDurationSpinner);
        controlPanel.add(loopCheckbox);
        controlPanel.add(addFramesButton);
        controlPanel.add(saveButton);

        previewPanel.setPreferredSize(new Dimension(300, 300));

        add(new JScrollPane(frameList), BorderLayout.WEST);
        add(controlPanel, BorderLayout.EAST);
        add(previewPanel, BorderLayout.CENTER);

        addFramesButton.addActionListener(e -> selectFrames());
        saveButton.addActionListener(e -> saveObject());

        previewTimer = new Timer((int) frameDurationSpinner.getValue(), e -> updatePreview());
        frameDurationSpinner.addChangeListener(e -> previewTimer.setDelay((int) frameDurationSpinner.getValue()));
    }

    private void selectFrames() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(Constants.CACHE_DIR + "/objects/"));
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            frameListModel.clear();
            previewFrames.clear();
            for (File file : chooser.getSelectedFiles()) {
                frameListModel.addElement(file);
                try {
                    previewFrames.add(ImageIO.read(file));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            currentFrame = 0;
            previewTimer.start();
        }
    }

    private void updatePreview() {
        if (this.previewFrames.size() >= 1) {
            currentFrame = (currentFrame + 1) % previewFrames.size();
        }
        previewPanel.repaint();
    }

    private void saveObject() {
        try {
            BufferedImage[] images = new BufferedImage[frameListModel.size()];
            String[] dirs = new String[images.length];
            for (int i = 0; i < images.length; i++) {
                File file = frameListModel.get(i);
                dirs[i] = file.toString();
                images[i] = ImageIO.read(file);
            }

            GameObject obj = new GameObject(0, 0, images[0].getWidth(), images[0].getHeight(), true, dirs);
            obj.loop = loopCheckbox.isSelected();
            obj.frameDuration = (int) frameDurationSpinner.getValue();

            JFileChooser saver = new JFileChooser();
            saver.setCurrentDirectory(new File(Constants.CACHE_DIR + "/mapdata/objects/"));
            if (saver.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = new File(saver.getSelectedFile().getAbsolutePath() + ".xml");

                // Save as XML
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.newDocument();

                Element objectElement = doc.createElement("object");
                objectElement.setAttribute("x", String.valueOf(obj.getX()));
                objectElement.setAttribute("y", String.valueOf(obj.getY()));
                objectElement.setAttribute("width", String.valueOf(obj.getWidth()));
                objectElement.setAttribute("height", String.valueOf(obj.getHeight()));
                objectElement.setAttribute("animated", String.valueOf(obj.isAnimated()));
                objectElement.setAttribute("loop", String.valueOf(obj.loop));
                objectElement.setAttribute("frameDuration", String.valueOf(obj.frameDuration));

                if (obj.dirs != null && obj.dirs.length > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < obj.dirs.length; i++) {
                        sb.append(obj.dirs[i]);
                        if (i < obj.dirs.length - 1) {
                            sb.append(",");
                        }
                    }
                    objectElement.setAttribute("dirs", sb.toString());
                }

                doc.appendChild(objectElement);

                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "no");
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");

                DOMSource source = new DOMSource(doc);
                StreamResult result = new StreamResult(selectedFile);
                transformer.transform(source, result);

                JOptionPane.showMessageDialog(this, "Object saved successfully.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error saving object: " + ex.getMessage());
        }
    }

    public static void open() {
        SwingUtilities.invokeLater(() -> new ObjectBuilder().setVisible(true));
    }
}
