package org.janelia.it.FlyWorkstation.shared.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import java.util.concurrent.CancellationException;

import javax.media.jai.operator.InvertDescriptor;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import loci.formats.FormatException;
import loci.formats.IFormatReader;
import loci.formats.gui.BufferedImageReader;
import loci.formats.in.*;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.shared.filestore.PathTranslator;
import org.janelia.it.FlyWorkstation.shared.workers.IndeterminateProgressMonitor;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common utilities for loading images, copying files, testing strings, etc.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Utils {

    private static final Logger log = LoggerFactory.getLogger(Utils.class);
    
    private static final int DEFAULT_BUFFER_SIZE = 1024;
    
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
    
    public static boolean areSame(Object obj1, Object obj2) {
    	return (obj1 == obj2) || (obj1!=null && obj2!=null && obj1.equals(obj2));
    }
    
    public static boolean areSameEntity(Entity entity1, Entity entity2) {
    	return areSame(entity1, entity2) || (entity1!=null && entity2!=null && entity1.getId().equals(entity2.getId()));
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
     * Read an image using the ImageIO API. Currently supports TIFFs, PNGs and JPEGs.
     * 
     * @param path
     * @return
     * @throws MalformedURLException
     */
    public static BufferedImage readImage(String path) throws Exception {
        try {
        	String format = path.substring(path.lastIndexOf(".")+1);
        	IFormatReader reader = null;
        	if (format.equals("tif") || format.equals("tiff")) {
        		reader = new TiffReader();
        	}
        	else if (format.equals("png")) {
        		reader = new APNGReader();
        	}
            else if (format.equals("jpg")||format.equals("jpeg")){
                reader = new JPEGReader();
            }
            else if (format.equals("bmp")){
                reader = new BMPReader();
            }
            else if (format.equals("gif")){
                reader = new GIFReader();
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
     * Read an image from a URL using the ImageIO API. Currently supports TIFFs, PNGs and JPEGs.
     * 
     * @param path
     * @return
     * @throws MalformedURLException
     */
    public static BufferedImage readImage(URL url) throws Exception {
        // Some extra finagling is required because LOCI libraries do not like the file protocol for some reason
        if (url.getProtocol().equals("file")) {
            String localFilepath = url.toString().replace("file:","");
            log.trace("loading cached file: {}", localFilepath);
            return Utils.readImage(localFilepath);
        }
        else {
            log.trace("loading url: {}", url);
            return Utils.readImage(url.toString());
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
     * Create an image from the source image, scaled by the width.
     *
     * @param sourceImage image to work against
     * @param size pixel size that the larger dimension should be
     * @return returns a BufferedImage to work with
     */
    public static BufferedImage getScaledImageByWidth(BufferedImage sourceImage, int width) {
        double scaledScale = (double) width / (double) sourceImage.getWidth();
        int newScaledHeight = (int) Math.round(scaledScale * sourceImage.getHeight());
        return Utils.getScaledImage(sourceImage, width, newScaledHeight);
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

    	if (type==0) {
    		type = BufferedImage.TYPE_INT_ARGB;
    	}
    	
    	if (type==BufferedImage.TYPE_BYTE_INDEXED || type==BufferedImage.TYPE_BYTE_BINARY) {
    		// Force the type to RGB in order to correctly display bitmapped images. This is strange, but it works.
    		type = BufferedImage.TYPE_INT_RGB;	
    	}
    	
        BufferedImage resizedImg = new BufferedImage(w, h, type);
        Graphics2D g2 = resizedImg.createGraphics();

    	if (((double)sourceImage.getHeight()/(double)h > 2) || ((double)sourceImage.getWidth()/(double)w > 2)) {
//    		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    	}
    	
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
    
    /**
     * Copy the input stream to the output stream, using a buffer of the given size. This method uses the old-style 
     * java.io calls.
     * @param input
     * @param output
     * @param bufferSize
     * @throws IOException
     */
	public static void copy(InputStream input, OutputStream output, int bufferSize) throws IOException {
		byte[] buf = new byte[bufferSize];
		int bytesRead = input.read(buf);
		while (bytesRead != -1) {
			output.write(buf, 0, bytesRead);
			bytesRead = input.read(buf);
		}
		output.flush();
	}

	/**
	 * Copy the input stream to the output stream, using a buffer of the given size. This method uses the new-style 
     * java.nio calls, and should be faster than copy(), in theory.
	 * @param input
	 * @param output
	 * @param bufferSize
	 * @throws IOException
	 */
    public static void copyNio(InputStream input, OutputStream output, int bufferSize) throws IOException {
		final ReadableByteChannel inputChannel = Channels.newChannel(input);
		final WritableByteChannel outputChannel = Channels.newChannel(output);
		fastChannelCopy(inputChannel, outputChannel, bufferSize);
		inputChannel.close();
		outputChannel.close();
	}

	/**
	 * Adapted from http://thomaswabner.wordpress.com/2007/10/09/fast-stream-copy-using-javanio-channels/
	 * @param src
	 * @param dest
	 * @throws IOException
	 */
    public static void fastChannelCopy(final ReadableByteChannel src, final WritableByteChannel dest, int bufferSize) throws IOException {
		final ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
		while (src.read(buffer) != -1) {
			// prepare the buffer to be drained
			buffer.flip();
			// write to the channel, may block
			dest.write(buffer);
			// If partial transfer, shift remainder down
			// If buffer is empty, same as doing clear()
			buffer.compact();
		}
		// EOF will leave buffer in fill state
		buffer.flip();
		// make sure the buffer is fully drained.
		while (buffer.hasRemaining()) {
			dest.write(buffer);
		}
	}
    
    /**
     * Cache the given file and then execute the callback once the file is available.
     * @param filePath
     * @param callback
     */
    private static void cacheAndProcessFileAsync(final String filePath, final FileCallable callback) {
        SimpleWorker worker = new SimpleWorker() {
            
            private File file;
            
            @Override
            protected void doStuff() throws Exception {
                file = SessionMgr.getCachedFile(filePath, false);
            }
            
            @Override
            protected void hadSuccess() {
                try {
                    if (callback!=null) {
                        callback.setParam(file);
                        callback.call();
                    }
                }
                catch (Exception e) {
                    hadError(e);
                }
            }
            
            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        worker.setProgressMonitor(new IndeterminateProgressMonitor(SessionMgr.getBrowser(), "Retrieving file...", ""));
        worker.execute();
    }
    
    /**
     * Run the FileCallable processing callback on the given file, either on the remote file directly, if the
     * remote file system is mounted, or after caching the file locally.
     * @param filePath
     * @param callback
     */
    public static void processStandardFilepath(final String filePath, final FileCallable callback) {

        final File file = new File(PathTranslator.convertPath(filePath));
        if (file.canRead()) {
            try {
                callback.call(file);
            }
            catch (Exception e) {
                SessionMgr.getSessionMgr().handleException(e);
            }
        }
        else {
            Utils.cacheAndProcessFileAsync(filePath, new FileCallable() {
                @Override
                public void call(File file) throws Exception {
                    if (file==null) {
                        JOptionPane.showMessageDialog(SessionMgr.getSessionMgr().getActiveBrowser(),
                                "Could not open file path", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    else {
                        callback.call(file);
                    }
                }
            });
        }
    }

    public static void copyURLToFile(String standardPath, File destination, SimpleWorker worker) throws Exception {

        log.info("standardPath: "+standardPath);
        log.info("destination: "+destination);
        
        //does destination directory exist ?
        if (destination.getParentFile() != null
            && !destination.getParentFile().exists()) {
            destination.getParentFile().mkdirs();
        }

        //make sure we can write to destination
        if (destination.exists() && !destination.canWrite()) {
            String message =
                "Unable to open file " + destination + " for writing.";
            throw new IOException(message);
        }

        WorkstationFile wfile = new WorkstationFile(standardPath);
        wfile.get();
        
        InputStream input = wfile.getStream();
        long length = wfile.getLength();
        
        log.info("Effective URL: "+wfile.getEffectiveURL());
        log.info("Length: "+length);
        
        if (length==0) {
            throw new Exception("Length of file was 0");
        }
        
        if (wfile.getStatusCode()!=200) {
            throw new Exception("Status code was "+wfile.getStatusCode());
        }
        
        try {
            FileOutputStream output = new FileOutputStream(destination);
            try {
                int copied = copy(input, output, length, worker);
                if (copied != length) {
                    throw new IOException("Bytes copied does not equal file length: "+copied+"!="+length);
                }
            } 
            finally {
                org.apache.commons.io.IOUtils.closeQuietly(output);
            }
        } 
        finally {
            org.apache.commons.io.IOUtils.closeQuietly(input);
        }
    }

    /**
     * Copied from Apache's commons-io, so that we could add progress indication
     */
    public static int copy(InputStream input, OutputStream output, long length, SimpleWorker worker) throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
            if (worker!=null) {
                worker.setProgress(count, length);
                if (worker.isCancelled()) throw new CancellationException();
            }
        }
        return count;
    }
}
