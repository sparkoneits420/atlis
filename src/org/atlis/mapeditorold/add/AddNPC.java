package org.atlis.mapeditorold.add;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Objects; 
import org.atlis.common.model.NPC;
import org.atlis.common.util.Constants;
import org.atlis.common.util.Utilities;
import org.atlis.mapeditorold.Screen;

public class AddNPC extends JFrame {

    private final DefaultListModel<File> npcListModel = new DefaultListModel<>();
    private final JList<File> npcList = new JList<>(npcListModel);
    private NPC selectedNPC;

    public AddNPC() {
        setTitle("Add NPC to Map");
        setSize(400, 400);
        Utilities.centerWindow(this);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JScrollPane scrollPane = new JScrollPane(npcList); 
        add(scrollPane, BorderLayout.CENTER);

        JButton selectButton = new JButton("Select NPC");
        add(selectButton, BorderLayout.SOUTH);

        npcList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        npcList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    selectNPC();
                }
            }
        });

        selectButton.addActionListener(e -> selectNPC());

        loadAvailableNPCs();
    }

    private void loadAvailableNPCs() {
        File dir = new File(Constants.CACHE_DIR + "/npcs/");
        System.out.println(dir);
        if (dir.exists() && dir.isDirectory()) {
            
            for (File file : Objects.requireNonNull(dir.listFiles((d, name) -> name.endsWith(".npc")))) {
                System.out.println(file);
                npcListModel.addElement(file);
            }
        }
    }

    private void selectNPC() {
        File selectedFile = npcList.getSelectedValue();
        if (selectedFile != null) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(selectedFile))) {
                selectedNPC = (NPC) ois.readObject(); 
                Screen.npcToAdd = selectedNPC;
                Screen.addingNPC = true;
                dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error loading NPC.");
            }
        }
    }

    public static void open() {
        SwingUtilities.invokeLater(() -> new AddNPC().setVisible(true));
    }
}