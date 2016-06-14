package org.janelia.it.workstation.shared.util;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import loci.formats.FormatException;
import loci.formats.IFormatReader;
import loci.formats.gui.BufferedImageReader;
import loci.formats.in.APNGReader;
import loci.formats.in.BMPReader;
import loci.formats.in.GIFReader;
import loci.formats.in.JPEGReader;
import loci.formats.in.TiffReader;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.filestore.PathTranslator;
import org.janelia.it.workstation.shared.workers.BackgroundWorker;
import org.janelia.it.workstation.shared.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.openide.windows.WindowManager;
import org.perf4j.LoggingStopWatch;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common utilities for loading images, copying files, testing strings, etc.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Utils {

    public static final String EXTENSION_LSM = "lsm";
    public static final String EXTENSION_BZ2 = "bz2";
    public static final String EXTENSION_LSM_BZ2 = EXTENSION_LSM + '.' + EXTENSION_BZ2;

    private static final Logger log = LoggerFactory.getLogger(Utils.class);

    private static final boolean TIMER = log.isDebugEnabled();

    private static final int ONE_KILOBYTE = 1024;
    private static final int ONE_MEGABYTE = 1024 * ONE_KILOBYTE;
    private static final int TEN_MEGABYTES = 10 * ONE_MEGABYTE;
    private static final int ONE_GIGABYTE = 1024 * ONE_MEGABYTE;

    private static final int DEFAULT_BUFFER_SIZE = 8 * ONE_KILOBYTE;

    public static ImageIcon grabOpenedIcon;
    public static ImageIcon grabClosedIcon;

    static {
        try {
            grabOpenedIcon = Utils.getClasspathImage("grab_opened.png");
            grabClosedIcon = Utils.getClasspathImage("grab_closed.png");
        }
        catch (FileNotFoundException e) {
            log.error("Could not find icons in classpath",e);
        }
    }

    public static boolean areSame(Object obj1, Object obj2) {
        return (obj1 == obj2) || (obj1 != null && obj2 != null && obj1.equals(obj2));
    }

    public static boolean areSameEntity(Entity entity1, Entity entity2) {
        return areSame(entity1, entity2) || (entity1 != null && entity2 != null && entity1.getId().equals(entity2.getId()));
    }

    public static Long getEntityIdFromUniqueId(String uniqueId) {
        String[] ids = uniqueId.split("/");
        String lastId = ids[ids.length - 1];
        if (!lastId.startsWith("e_")) {
            throw new IllegalStateException("uniqueId must end with entity id starting with 'e_': " + uniqueId);
        }
        return Long.parseLong(lastId.substring(2));
    }

    public static String getParentIdFromUniqueId(String uniqueId) {
        if (uniqueId == null) {
            return null;
        }
        String[] ids = uniqueId.split("/");
        StringBuilder parentUniqueId = new StringBuilder();
        try {
            for (int i = 1; i < ids.length - 2; i++) {
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
            if (renderer == null) {
                renderer = defaultRenderer;
            }

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
     */
    public static BufferedImage readImage(String path) throws Exception {
        try {
            String selectedRenderer = (String) SessionMgr.getSessionMgr().getModelProperty(SessionMgr.DISPLAY_RENDERER_2D);

            RendererType2D renderer = selectedRenderer == null ? RendererType2D.LOCI : RendererType2D.valueOf(selectedRenderer);
            BufferedImage image = null;

            if (renderer == RendererType2D.IMAGE_IO) {

                InputStream stream = null;
                GetMethod get = null;
                try {

                    if (path.startsWith("http://")) {
                        HttpClient client = SessionMgr.getSessionMgr().getWebDavClient().getHttpClient();
                        get = new GetMethod(path);
                        int responseCode = client.executeMethod(get);
                        log.trace("readImage: GET " + responseCode + ", path=" + path);
                        if (responseCode != 200) {
                            throw new FileNotFoundException("Response code "+responseCode+" returned for call to "+path);
                        }
                        stream = get.getResponseBodyAsStream();
                    }
                    else {
                        log.trace("readImage: FileInputStream path=" + path);
                        stream = new FileInputStream(new File(path));
                    }

                    // Supports GIF, PNG, JPEG, BMP, and WBMP 
                    image = ImageIO.read(stream);
                }
                finally {
                    if (get != null) {
                        get.releaseConnection();
                    }
                    if (stream != null) {
                        try {
                            stream.close();
                        }
                        catch (IOException e) {
                            log.warn("readImage: failed to close {}", path, e);
                        }
                    }
                }
            }
            else {
                String format = path.substring(path.lastIndexOf(".") + 1);
                IFormatReader reader;
                switch (format) {
                    case "tif":
                    case "tiff":
                        reader = new TiffReader();
                        break;
                    case "png":
                        reader = new APNGReader();
                        break;
                    case "jpg":
                    case "jpeg":
                        reader = new JPEGReader();
                        break;
                    case "bmp":
                        reader = new BMPReader();
                        break;
                    case "gif":
                        reader = new GIFReader();
                        break;
                    default:
                        throw new FormatException("File format is not supported: " + format);
                }
                BufferedImageReader in = new BufferedImageReader(reader);
                in.setId(path);
                image = in.openImage(0);
                in.close();
            }

            return image;
        }
        catch (Exception e) {
            if (e instanceof IOException) {
                throw e;
            } else {
                throw new IOException("Error reading image: " + path, e);
            }
        }
    }

    /**
     * Read an image from a URL using the ImageIO API. Currently supports TIFFs, PNGs and JPEGs.
     */
    public static BufferedImage readImage(URL url) throws Exception {
        BufferedImage image;
        StopWatch stopWatch = TIMER ? new LoggingStopWatch() : null;
        // Some extra finagling is required because LOCI libraries do not like the file protocol for some reason
        if (url.getProtocol().equals("file")) {
            String localFilepath = url.toString().replace("file:", "");
            log.trace("Loading cached file: {}", localFilepath);
            image = Utils.readImage(localFilepath);
            if (TIMER) {
                stopWatch.stop("readCachedImage");
            }
        }
        else {
            log.trace("Loading url: {}", url);
            image = Utils.readImage(url.toString());
            if (TIMER) {
                stopWatch.stop("readRemoteImage");
            }
        }
        return image;
    }

    /**
     * Create an image from the source image, scaled at the given percentage.
     *
     * @param sourceImage image to work against
     * @param scale percentage to change the image
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
     * @param  sourceImage  image to work against
     * @param  width        pixel size that the larger dimension should be
     * @return a BufferedImage to work with
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
     * @param w - desired width
     * @param h - desired height
     * @return - the new resized image
     */
    public static BufferedImage getScaledImage(BufferedImage sourceImage, int w, int h) {
        StopWatch stopWatch = TIMER ? new LoggingStopWatch() : null;

        int type = sourceImage.getType();

        if (type == 0) {
            type = BufferedImage.TYPE_INT_ARGB;
        }

        if (type == BufferedImage.TYPE_BYTE_INDEXED || type == BufferedImage.TYPE_BYTE_BINARY) {
            // Force the type to RGB in order to correctly display bitmapped images. This is strange, but it works.
            type = BufferedImage.TYPE_INT_RGB;
        }

        BufferedImage resizedImg = new BufferedImage(w, h, type);
        Graphics2D g2 = resizedImg.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(sourceImage, 0, 0, w, h, null);
        g2.dispose();
        if (TIMER) {
            stopWatch.stop("getScaledImage");
        }
        return resizedImg;
    }

    /**
     * This method returns true if the specified image has transparent pixels.
     * From http://www.exampledepot.com/egs/java.awt.image/HasAlpha.html
     */
    public static boolean hasAlpha(Image image) {
        // If buffered image, the color model is readily available
        if (image instanceof BufferedImage) {
            BufferedImage bimage = (BufferedImage) image;
            return bimage.getColorModel().hasAlpha();
        }

        // Use a pixel grabber to retrieve the image's color model;
        // grabbing a single pixel is usually sufficient
        PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
        try {
            pg.grabPixels();
        } catch (InterruptedException e) {
            log.warn("failed to grab pixels for " + image, e);
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
            return (BufferedImage) image;
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
        }
        catch (HeadlessException e) {
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
        JFrame mainFrame = (JFrame)WindowManager.getDefault().getMainWindow(); 
        if (component==mainFrame) {
            setMainFrameCursorWaitStatus(true);
            return;
        }
        component.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    public static void setDefaultCursor(Component component) {
        JFrame mainFrame = (JFrame) WindowManager.getDefault().getMainWindow();
        if (component == mainFrame) {
            setMainFrameCursorWaitStatus(false);
            return;
        }
        component.setCursor(Cursor.getDefaultCursor());
    }

    /**
     * Adapted from http://netbeans-org.1045718.n5.nabble.com/Setting-wait-cursor-td3026613.html
     */
    public static void setMainFrameCursorWaitStatus(final boolean isWaiting) {
        try {
            JFrame mainFrame = (JFrame) WindowManager.getDefault().getMainWindow();
            Component glassPane = mainFrame.getGlassPane();
            if (isWaiting) {
                glassPane.setVisible(true);
                glassPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            }
            else {
                glassPane.setVisible(false);
                glassPane.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        }
        catch (Exception e) {
            log.error("Error changing main frame cursor wait status",e);
        }
    }
    
    public static void queueWaitingCursor(final Component component) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                setWaitingCursor(component);
            }
        });
    }

    public static void queueDefaultCursor(final Component component) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                setDefaultCursor(component);
            }
        });
    }

    /**
     * Copy the input stream to the output stream, using a buffer of the given size. This method uses the old-style
     * java.io calls.
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
                    if (callback != null) {
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
        worker.setProgressMonitor(new IndeterminateProgressMonitor(SessionMgr.getMainFrame(), "Retrieving file...", ""));
        worker.execute();
    }

    /**
     * Run the FileCallable processing callback on the given file, either on the remote file directly, if the
     * remote file system is mounted, or after caching the file locally.
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
                    if (file == null) {
                        JOptionPane.showMessageDialog(SessionMgr.getMainFrame(),
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

        log.info("copyURLToFile: entry, standardPath={}, destination={}", standardPath, destination);

        final File destinationDir = destination.getParentFile();
        if ((destinationDir != null) && (! destinationDir.exists())) {
            Files.createDirectories(destinationDir.toPath());
        }

        //make sure we can write to destination
        if (destination.exists() && !destination.canWrite()) {
            throw new IOException("Unable to open " + destination.getAbsolutePath() + " for writing.");
        }

        @SuppressWarnings("UnusedAssignment") InputStream input = null;

        WorkstationFile wfile = new WorkstationFile(standardPath);

        try {
            input = wfile.getStream();
            Long length = wfile.getLength();

            log.info("copyURLToFile: length={}, effectiveURL={}", length, wfile.getEffectiveURL());

            if (length != null && length == 0) {
                throw new IOException("length of " + wfile.getEffectiveURL() + " is 0");
            }

            if (wfile.getStatusCode() != 200) {
                throw new IOException("status code for " + wfile.getEffectiveURL() + " is " + wfile.getStatusCode());
            }

            int estimatedCompressionFactor = 1;
            if (standardPath.endsWith(EXTENSION_BZ2) &&
                    (! destination.getName().endsWith(EXTENSION_BZ2))) {
                input = new BZip2CompressorInputStream(input, true);
                estimatedCompressionFactor = 3;
            }

            FileOutputStream output = new FileOutputStream(destination);
            try {
                final long totalBytesWritten = copy(input, output, length==null?100:length, worker, estimatedCompressionFactor);
                if (length != null && totalBytesWritten < length) {
                    throw new IOException("bytes written (" + totalBytesWritten + ") for " + wfile.getEffectiveURL() +
                                          " is less than source length (" + length + ")");
                }
            } finally {
                IOUtils.closeQuietly(input); // close input here to ensure bzip stream is properly closed
                IOUtils.closeQuietly(output);
            }
        } finally {
            wfile.close();
        }
    }

    /**
     * Adapted from Apache's commons-io, so that we could add progress percentage and status.
     */
    private static long copy(InputStream input,
                             OutputStream output,
                             long length,
                             SimpleWorker worker,
                             int estimatedCompressionFactor) throws IOException {

        BackgroundWorker backgroundWorker = null;
        String backgroundStatus = null;
        if (worker instanceof BackgroundWorker) {
            backgroundWorker = (BackgroundWorker) worker;
            backgroundStatus = backgroundWorker.getStatus();
            if (backgroundStatus == null) {
                backgroundStatus = "copying file (";
            } else {
                backgroundStatus = backgroundStatus + " (";
            }
        }

        final long startTime = System.currentTimeMillis();
        final long estimatedLength = estimatedCompressionFactor * length;

        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int bytesRead;
        long totalBytesWritten = 0;
        long totalBytesWrittenAtLastStatusUpdate = totalBytesWritten;
        long totalMegabytesWritten;
        while (-1 != (bytesRead = input.read(buffer))) {

            output.write(buffer, 0, bytesRead);
            totalBytesWritten += bytesRead;

            if (worker != null) {

                worker.throwExceptionIfCancelled();

                if ((totalBytesWritten - totalBytesWrittenAtLastStatusUpdate) > TEN_MEGABYTES) {

                    totalBytesWrittenAtLastStatusUpdate = totalBytesWritten;

                    if (totalBytesWritten < estimatedLength) {
                        worker.setProgress(totalBytesWritten, estimatedLength);
                    }

                    if (backgroundWorker != null) {
                        totalMegabytesWritten = totalBytesWritten / ONE_MEGABYTE;
                        backgroundWorker.setStatus(backgroundStatus + totalMegabytesWritten + " Mb written)");
                    }
                }
            }
        }

        if (worker != null) {

            worker.setProgress(totalBytesWritten, totalBytesWritten);

            if (backgroundWorker != null) {
                totalMegabytesWritten = totalBytesWritten / ONE_MEGABYTE;
                backgroundWorker.setStatus(backgroundStatus + totalMegabytesWritten + " Mb written)");
            }
        }

        final long elapsedTime = System.currentTimeMillis() - startTime;

        if (log.isInfoEnabled()) {
            final BigDecimal elapsedSeconds = divideAndScale(elapsedTime, 1000, 1);
            BigDecimal amountWritten;
            String amountUnits;
            if (totalBytesWritten > ONE_GIGABYTE) {
                amountWritten = divideAndScale(totalBytesWritten, ONE_GIGABYTE, 1);
                amountUnits = " gigabytes in ";
            } else if (totalBytesWritten > ONE_MEGABYTE) {
                amountWritten = divideAndScale(totalBytesWritten, ONE_MEGABYTE, 1);
                amountUnits = " megabytes in ";
            } else {
                amountWritten = divideAndScale(totalBytesWritten, ONE_KILOBYTE, 1);
                amountUnits = " kilobytes in ";
            }
            log.info("copy: wrote " + amountWritten + amountUnits + elapsedSeconds + " seconds");
        }

        return totalBytesWritten;
    }

    private static BigDecimal divideAndScale(double numerator, double denominator, int scale) {
        return new BigDecimal(numerator / denominator).setScale(scale, BigDecimal.ROUND_HALF_UP);
    }

    public static boolean hasAncestorWithType(Component component, Class<?> clazz) {
        if (clazz==null) return false;
        Component c = component;
        while (c!=null) {
            log.trace("check if {} is assignable from {}",clazz.getName(),c.getClass().getName());
            if (clazz.isAssignableFrom(c.getClass())) {
                return true;
            }
            c = c.getParent();
        }
        return false;
    }

    public static <T> T getAncestorWithType(Component component, Class<T> clazz) {
        if (clazz==null) return null;
        Component c = component;
        while (c!=null) {
            log.trace("check if {} is assignable from {}",clazz.getName(),c.getClass().getName());
            if (clazz.isAssignableFrom(c.getClass())) {
                return (T)c;
            }
            c = c.getParent();
        }
        return null;
    }
}
