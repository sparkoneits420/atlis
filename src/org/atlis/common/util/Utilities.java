package org.atlis.common.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

public class Utilities {

    public static long intsToLong(int j, int i) {
        return ((long) j << 32) | ((i & 0xffffffffL));
    }

    public static int[] longToInts(long l) {
        int[] ints = new int[2];
        ints[0] = (int) (l >> 32);
        ints[1] = (int) l;
        return ints;
    }

    public static Image filterRGBA(Image image) {
        ImageFilter filter = new RGBImageFilter() {
            public int markerRGB = Color.MAGENTA.getRGB() | 0xFF000000;

            @Override
            public final int filterRGB(int x, int y, int rgb) {
                if ((rgb | 0xFF000000) == markerRGB) {
                    return 0x00FFFFFF & rgb;
                } else {
                    return rgb;
                }
            }
        };
        ImageProducer imageProducer = new FilteredImageSource(image.getSource(), filter);
        return Toolkit.getDefaultToolkit().createImage(imageProducer);
    }

    public static int[] imageToIntArray(Image img) {
        if (!(img instanceof BufferedImage)) {
            BufferedImage bimage = new BufferedImage(
                    img.getWidth(null),
                    img.getHeight(null),
                    BufferedImage.TYPE_INT_ARGB
            );
            Graphics2D g2d = bimage.createGraphics();
            g2d.drawImage(img, 0, 0, null);
            g2d.dispose();
            img = bimage;
        }

        BufferedImage buffered = (BufferedImage) img;
        int width = buffered.getWidth();
        int height = buffered.getHeight();
        return buffered.getRGB(0, 0, width, height, null, 0, width);
    }

    public static Image intArrayToImage(int[] pixels, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, pixels, 0, width);
        return image;
    }

    public static void centerWindow(Window frame) {
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (int) ((dimension.getWidth() - frame.getWidth()) / 2);
        int y = (int) ((dimension.getHeight() - frame.getHeight()) / 2);
        frame.setLocation(x, y);
    }

    public static BufferedImage scaleImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage scaledImage = new BufferedImage(targetWidth, targetHeight, originalImage.getType());
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        return scaledImage;
    }

    public static int generateUID(String username) {
        CRC32 crc = new CRC32();
        crc.update(username.getBytes(StandardCharsets.UTF_8));
        return (int) crc.getValue();  
    }
}
