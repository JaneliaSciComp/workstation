/*
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/26/11
 * Time: 7:16 PM
 */
package org.janelia.it.FlyWorkstation.shared.util;

import loci.formats.gui.BufferedImageReader;
import loci.formats.in.TiffReader;

import javax.media.jai.operator.InvertDescriptor;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.janelia.it.FlyWorkstation.gui.util.ConsoleProperties;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Common utilities for loading images, testing strings, etc.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Utils {

    private static final String JACS_DATA_PATH_MAC = ConsoleProperties.getString("remote.defaultMacPath");
    private static final String JACS_DATA_PATH_LINUX = ConsoleProperties.getString("remote.defaultLinuxPath");
    
    public static ImageIcon grabOpenedIcon;
    public static ImageIcon grabClosedIcon;

    static {
        try {
            grabOpenedIcon = Utils.getClasspathImage("grab_opened.png");
            grabClosedIcon = Utils.getClasspathImage("grab_closed.png");
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static boolean isEmpty(String str) {
        return (str == null || "".equals(str));
    }

    public static String convertJacsPathLinuxToMac(String filepath) {
        return filepath.replace(JACS_DATA_PATH_LINUX, JACS_DATA_PATH_MAC);
    }
    
    /**
     * Borrowed from http://www.pikopong.com/blog/2008/08/13/auto-resize-jtable-column-width/
     *
     * @deprecated this is now part of DynamicTable, which should be used instead
     * @param table table to work against
     */
    public static void autoResizeColWidth(JTable table) {

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        DefaultTableCellRenderer defaultRenderer = (DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer();

        int margin = 5;

        for (int i = 0; i < table.getColumnCount(); i++) {
            DefaultTableColumnModel colModel = (DefaultTableColumnModel) table.getColumnModel();
            TableColumn col = colModel.getColumn(i);
            int width;

            // Get width of column header
            TableCellRenderer renderer = col.getHeaderRenderer();
            if (renderer == null) renderer = defaultRenderer;

            Component comp = renderer.getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, 0);
            width = comp.getPreferredSize().width;

            // Get maximum width of column data
            for (int r = 0; r < table.getRowCount(); r++) {
                renderer = table.getCellRenderer(r, i);
                comp = renderer.getTableCellRendererComponent(table, table.getValueAt(r, i), false, false, r, i);
                width = Math.max(width, comp.getPreferredSize().width);
            }

            width += 2 * margin;
            col.setPreferredWidth(width);
        }

        defaultRenderer.setHorizontalAlignment(SwingConstants.LEFT);
    }

    /**
     * Load an image that is found in the /images directory within the classpath.
     *
     * @param filename
     * @return
     * @throws FileNotFoundException
     */
    public static ImageIcon getClasspathImage(String filename) throws FileNotFoundException {
        try {
            URL picURL = Utils.class.getResource("/images/" + filename);
            return new ImageIcon(picURL);
        }
        catch (Exception e) {
            throw new FileNotFoundException("/images/" + filename);
        }
    }

    /**
     * Read an image using the ImageIO API. Supports TIFFs.
     *
     * @param path
     * @return
     * @throws MalformedURLException
     */
    public static BufferedImage readImage(String path) throws IOException {
        try {
            BufferedImageReader in = new BufferedImageReader(new TiffReader());
            in.setId(path);
            BufferedImage image = in.openImage(0);
            in.close();
            return image;
        }
        catch (Exception e) {
            if (e instanceof IOException) throw (IOException) e;
            throw new IOException("Error reading image", e);
        }
    }

    /**
     * Returns a color inverted version of the given image.
     *
     * @param image
     * @return
     */
    public static BufferedImage invertImage(BufferedImage image) {
        RenderingHints hints = new RenderingHints(null);
        return InvertDescriptor.create(image, hints).getAsBufferedImage();
    }

    /**
     * Create an image from the source image, scaled at the given percentage.
     *
     * @param sourceImage image to work against
     * @param scale       percentage to change the image
     * @return returns a BufferedImage to work with
     */
    public static BufferedImage getScaledImageIcon(BufferedImage sourceImage, double scale) {
        int newWidth = (int) Math.round(scale * sourceImage.getWidth());
        int newHeight = (int) Math.round(scale * sourceImage.getHeight());
        return getScaledImage(sourceImage, newWidth, newHeight);
    }

    /**
     * Create an image from the source image, scaled with the larger dimension.
     *
     * @param sourceImage image to work against
     * @param size pixel size that the larger dimension should be
     * @return returns a BufferedImage to work with
     */
    public static BufferedImage getScaledImageIcon(BufferedImage sourceImage, int size) {
        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();
        int newWidth = size;
        int newHeight = size;
        if (width > height) {
            double scale = (double) newWidth / (double) width;
            newHeight = (int) Math.round(scale * height);
        }
        else if (width < height) {
            double scale = (double) newHeight / (double) height;
            newWidth = (int) Math.round(scale * width);
        }
        return getScaledImage(sourceImage, newWidth, newHeight);
    }

    /**
     * Resizes an image using a Graphics2D object backed by a BufferedImage.
     *
     * @param sourceImage - source image to scale
     * @param w           - desired width
     * @param h           - desired height
     * @return - the new resized image
     */
    public static BufferedImage getScaledImage(BufferedImage sourceImage, int w, int h) {
        BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = resizedImg.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(sourceImage, 0, 0, w, h, null);
        g2.dispose();
        return resizedImg;
    }

    public static void setWaitingCursor(Component component) {
        component.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    public static void setDefaultCursor(Component component) {
        component.setCursor(Cursor.getDefaultCursor());
    }

    public static void setOpenedHandCursor(Component component) {
        Cursor grabClosedCursor = Toolkit.getDefaultToolkit().createCustomCursor(grabOpenedIcon.getImage(), new Point(0, 0), "img");
        component.setCursor(grabClosedCursor);
    }

    public static void setClosedHandCursor(Component component) {
//    	component.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        Cursor grabClosedCursor = Toolkit.getDefaultToolkit().createCustomCursor(grabClosedIcon.getImage(), new Point(0, 0), "img");
        component.setCursor(grabClosedCursor);
    }
}
