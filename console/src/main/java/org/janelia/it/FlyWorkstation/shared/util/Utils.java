/*
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/26/11
 * Time: 7:16 PM
 */
package org.janelia.it.FlyWorkstation.shared.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.media.jai.operator.InvertDescriptor;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import loci.formats.FormatException;
import loci.formats.IFormatReader;
import loci.formats.gui.BufferedImageReader;
import loci.formats.in.APNGReader;
import loci.formats.in.TiffReader;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * Common utilities for loading images, testing strings, etc.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Utils {
    
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

    public static String getFilePath(Entity entity) {
    	if (entity == null) return null;
    	return entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
    }
    
    public static String getDefaultImageFilePath(Entity entity) {

    	String type = entity.getEntityType().getName();
    	String path = null;
    	
    	// If the entity is a 2D image, just return its path
		if (type.equals(EntityConstants.TYPE_IMAGE_2D)) {
			path = getFilePath(entity);
		}
    	
		if (path == null) {
	    	// If the entity has a default 2D image, just return that path
	    	path = getFilePath(entity.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE));
		}

		if (path == null) {
	    	// TODO: This is for backwards compatibility with old data. Remove this in the future.
	    	path = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH);
		}
    	
		return path;
    }

    public static String getAnyFilePath(Entity entity) {
    	String filePath = getFilePath(entity);
    	if (filePath != null) {
    		return filePath;
    	}
    	return getDefaultImageFilePath(entity);
    }
    
    public static boolean areSame(Object obj1, Object obj2) {
    	return (obj1 == obj2) || (obj1!=null && obj2!=null && obj1.equals(obj2));
    }
    
    public static boolean areSameEntity(Entity entity1, Entity entity2) {
    	return areSame(entity1, entity2) || (entity1!=null && entity2!=null && entity1.getId().equals(entity2.getId()));
    }
    
    public static boolean isEmpty(String str) {
        return (str == null || "".equals(str));
    }
    
    public static String join(List list, String delim) {
    	StringBuffer buf = new StringBuffer();
    	for(Object obj : list) {
    		if (buf.length()>0) buf.append(delim);
    		buf.append(obj.toString());
    	}	
    	return buf.toString();
    }
    
    public static Long getEntityIdFromUniqueId(String uniqueId) {
    	String[] ids = uniqueId.split("/");
    	String lastId = ids[ids.length-1];
    	if (!lastId.startsWith("e_")) {
    		throw new IllegalStateException("uniqueId must end with entity id starting with 'e_': "+uniqueId);
    	}
    	return Long.parseLong(lastId.substring(2));
    }

    public static String getParentIdFromUniqueId(String uniqueId) {
    	if (uniqueId==null) return null;
    	String[] ids = uniqueId.split("/");
    	StringBuffer parentUniqueId = new StringBuffer();
    	try {
        	for(int i=1; i<ids.length-2; i++) {
        		parentUniqueId.append("/");
        		parentUniqueId.append(ids[i]);
        	}	
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    		return null;
    	}
    	return parentUniqueId.toString();
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
     * Read an image using the ImageIO API. Currently supports TIFFs and PNGs.
     * 
     * @param path
     * @return
     * @throws MalformedURLException
     */
    public static BufferedImage readImage(String path) throws Exception {
        try {
        	String format = path.substring(path.lastIndexOf(".")+1);
        	IFormatReader reader = null;
        	if (format.equals("tif")) {
        		reader = new TiffReader();
        	}
        	else if (format.equals("png")) {
        		reader = new APNGReader();
        	}
        	else {
        		throw new FormatException("File format is not supported: "+format);
        	}
            BufferedImageReader in = new BufferedImageReader(reader);
            in.setId(path);            
            BufferedImage image = in.openImage(0);
            in.close();
            return image;
        }
        catch (Exception e) {
            if (e instanceof IOException) throw (IOException) e;
            throw new IOException("Error reading image: "+path, e);
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
    public static BufferedImage getScaledImage(BufferedImage sourceImage, double scale) {
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
    public static BufferedImage getScaledImage(BufferedImage sourceImage, int size) {
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
    	int type = sourceImage.getType();
    	if (type==0) type = BufferedImage.TYPE_INT_ARGB;
        BufferedImage resizedImg = new BufferedImage(w, h, type);
        Graphics2D g2 = resizedImg.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(sourceImage, 0, 0, w, h, null);
        g2.dispose();
        return resizedImg;
    }
    
    /**
     * This method returns true if the specified image has transparent pixels.
     * From http://www.exampledepot.com/egs/java.awt.image/HasAlpha.html
     */
    public static boolean hasAlpha(Image image) {
        // If buffered image, the color model is readily available
        if (image instanceof BufferedImage) {
            BufferedImage bimage = (BufferedImage)image;
            return bimage.getColorModel().hasAlpha();
        }

        // Use a pixel grabber to retrieve the image's color model;
        // grabbing a single pixel is usually sufficient
        PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
        try {
            pg.grabPixels();
        } 
        catch (InterruptedException e) {
        }

        // Get the image's color model
        ColorModel cm = pg.getColorModel();
        return cm.hasAlpha();
    }
    
    /**
     * This method returns a buffered image with the contents of an image.
     * From http://www.exampledepot.com/egs/java.awt.image/Image2Buf.html
     */
    public static BufferedImage toBufferedImage(Image image) {
        if (image instanceof BufferedImage) {
            return (BufferedImage)image;
        }

        // This code ensures that all the pixels in the image are loaded
        image = new ImageIcon(image).getImage();

        // Determine if the image has transparent pixels; for this method's
        // implementation, see Determining If an Image Has Transparent Pixels
        boolean hasAlpha = hasAlpha(image);

        // Create a buffered image with a format that's compatible with the screen
        BufferedImage bimage = null;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try {
            // Determine the type of transparency of the new buffered image
            int transparency = Transparency.OPAQUE;
            if (hasAlpha) {
                transparency = Transparency.BITMASK;
            }
            
            // Create the buffered image
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            bimage = gc.createCompatibleImage(
                image.getWidth(null), image.getHeight(null), transparency);
        } catch (HeadlessException e) {
            // The system does not have a screen
        }

        if (bimage == null) {
            // Create a buffered image using the default color model
            int type = BufferedImage.TYPE_INT_RGB;
            if (hasAlpha) {
                type = BufferedImage.TYPE_INT_ARGB;
            }
            bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
        }

        // Copy image to buffered image
        Graphics g = bimage.createGraphics();

        // Paint the image onto the buffered image
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return bimage;
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
        Cursor grabClosedCursor = Toolkit.getDefaultToolkit().createCustomCursor(grabClosedIcon.getImage(), new Point(0, 0), "img");
        component.setCursor(grabClosedCursor);
    }
}
