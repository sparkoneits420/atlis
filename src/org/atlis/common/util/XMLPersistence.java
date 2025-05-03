package org.atlis.common.util;

import java.awt.Image;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import javax.imageio.ImageIO;
import static org.atlis.common.model.Tile.GRASS;
import static org.atlis.common.model.Tile.STONE;
import org.atlis.common.model.GameObject;
import org.atlis.common.model.Region;
import org.atlis.common.model.Tile;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;

public class XMLPersistence {

    public static void saveTextFile(ArrayList<String> strings, String url) {
        try {
            File file = new File(url);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream out = new FileOutputStream(file);
            for (String s : strings) {
                out.write(s.getBytes());
                out.write('\n');
            }
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static HashMap<Integer, Image> tileImages = null;

    public static HashMap<Integer, Image> getTileImages() {
        if (tileImages == null) {
            tileImages = new HashMap<>();
            try {
                tileImages.put(GRASS, ImageIO.read(new File(Constants.CACHE_DIR + "/tiles/grass.png")));
                tileImages.put(STONE, ImageIO.read(new File(Constants.CACHE_DIR + "/tiles/stone.png")));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return tileImages;
    }

    public static void saveXML(Region region) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            Document doc = dBuilder.newDocument();
            Element rootElement = doc.createElement("region");
            rootElement.setAttribute("id", String.valueOf(region.getId()));
            doc.appendChild(rootElement);

            // Save objects
            for (GameObject object : region.getObjects()) {
                Element objElement = doc.createElement("object");
                objElement.setAttribute("x", String.valueOf(object.getX()));
                objElement.setAttribute("y", String.valueOf(object.getY()));
                objElement.setAttribute("width", String.valueOf(object.getWidth()));
                objElement.setAttribute("height", String.valueOf(object.getHeight()));
                objElement.setAttribute("animated", String.valueOf(object.isAnimated()));

                if (object.dirs != null && object.dirs.length > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < object.dirs.length; i++) {
                        sb.append(object.dirs[i]);
                        if (i < object.dirs.length - 1) {
                            sb.append(",");
                        }
                    }
                    objElement.setAttribute("dirs", sb.toString());
                }

                rootElement.appendChild(objElement);
            }

            // Save tiles
            for (Tile tile : region.values()) {
                if (tile != null && tile.getType() != 0) {
                    Element tileElement = doc.createElement("tile");
                    tileElement.setAttribute("x", String.valueOf(tile.x));
                    tileElement.setAttribute("y", String.valueOf(tile.y));
                    tileElement.setAttribute("type", String.valueOf(tile.getType()));
                    rootElement.appendChild(tileElement);
                }
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            // === Pretty format settings ===
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4"); // Indent 4 spaces
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");

            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(Constants.CACHE_DIR + "/mapdata/" + region.getId() + ".xml"));

            transformer.transform(source, result);
            System.out.println("Saved region successfully.. (pretty printed)");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static Region loadXML(String filePath) {
        try {
            File xmlFile = new File(filePath);
            if (!xmlFile.exists()) {
                System.out.println("File doesn't exist: " + xmlFile.getPath());
                return null;
            }

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();
            long regionId = Long.parseLong(root.getAttribute("id"));
            Region region = new Region(regionId);

            NodeList nodeList = root.getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element elem = (Element) node;
                    if (elem.getTagName().equals("object")) {
                        int x = Integer.parseInt(elem.getAttribute("x"));
                        int y = Integer.parseInt(elem.getAttribute("y"));
                        int width = Integer.parseInt(elem.getAttribute("width"));
                        int height = Integer.parseInt(elem.getAttribute("height"));
                        boolean animated = Boolean.parseBoolean(elem.getAttribute("animated"));

                        String[] dirs = new String[0];
                        if (elem.hasAttribute("dirs")) {
                            String dirsString = elem.getAttribute("dirs");
                            dirs = dirsString.split(",");
                        }

                        region.addObject(new GameObject(x, y, width, height, animated, dirs));
                    } else if (elem.getTagName().equals("tile")) {
                        int x = Integer.parseInt(elem.getAttribute("x"));
                        int y = Integer.parseInt(elem.getAttribute("y"));
                        int type = Integer.parseInt(elem.getAttribute("type"));

                        Tile tile = new Tile(x, y, type, regionId);
                        region.put(Utilities.intsToLong(x, y), tile);
                    }
                }
            }

            return region;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println("Couldn't load region");
        return null;
    }
}
