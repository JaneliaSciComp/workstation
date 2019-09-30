package org.janelia.workstation.core.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import com.google.common.io.ByteStreams;
import loci.common.ByteArrayHandle;
import loci.common.Location;
import loci.formats.FormatException;
import loci.formats.IFormatReader;
import loci.formats.gui.BufferedImageReader;
import loci.formats.in.APNGReader;
import loci.formats.in.BMPReader;
import loci.formats.in.GIFReader;
import loci.formats.in.JPEGReader;
import loci.formats.in.TiffReader;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.janelia.filecacheutils.FileProxy;
import org.janelia.workstation.core.api.FileMgr;
import org.janelia.workstation.core.options.OptionConstants;
import org.janelia.workstation.core.workers.BackgroundWorker;
import org.janelia.workstation.core.workers.IndeterminateProgressMonitor;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
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

    private static final Logger log = LoggerFactory.getLogger(Utils.class);
    private static final boolean TIMER = log.isTraceEnabled();

    // Semi, poor-man's feature toggle.
    public static final boolean SUPPORT_NEURON_SEPARATION_PARTIAL_DELETION_IN_GUI = true;
    public static final String EXTENSION_LSM = "lsm";
    public static final String EXTENSION_BZ2 = "bz2";
    public static final String EXTENSION_LSM_BZ2 = EXTENSION_LSM + '.' + EXTENSION_BZ2;

    private static final int ONE_KILOBYTE = 1024;
    private static final int ONE_MEGABYTE = 1024 * ONE_KILOBYTE;
    private static final int TEN_MEGABYTES = 10 * ONE_MEGABYTE;
    private static final int ONE_GIGABYTE = 1024 * ONE_MEGABYTE;

    private static final int DEFAULT_BUFFER_SIZE = 8 * ONE_KILOBYTE;

    static {
        // Scan to find the Twelvemonkeys plugin for TIFF
        ImageIO.scanForPlugins();
    }

    public static BufferedImage readImageFromInputStream(InputStream inputStream, String format) {
        String selectedRenderer = FrameworkAccess.getModelProperty(
                OptionConstants.DISPLAY_RENDERER_2D, RendererType2D.LOCI.toString());
        RendererType2D renderer = RendererType2D.valueOf(selectedRenderer);
        BufferedImage image;
        if (renderer == RendererType2D.IMAGE_IO) {
            try {
                image = readWithImageIOFromInputStream(inputStream);
            } catch (IOException e) {
                throw new IllegalArgumentException("Invalid image stream", e);
            }
        } else {
            try {
                byte[] imageBytes = ByteStreams.toByteArray(inputStream);
                String streamId = "inBytes." + format;
                Location.mapFile(streamId, new ByteArrayHandle(imageBytes));
                image = readWithLociReaderFromStreamId(streamId, format);
            } catch (IOException e) {
                throw new IllegalArgumentException("Invalid image stream", e);
            }
        }
        if (image == null) {
            throw new IllegalArgumentException("File format is not supported: " + format);
        }
        return image;
    }

    private static BufferedImage readWithImageIOFromInputStream(InputStream inputStream) throws IOException {
        // Supports GIF, PNG, JPEG, BMP, and WBMP
        return ImageIO.read(inputStream);
    }

    private static BufferedImage readWithLociReaderFromStreamId(String streamId, String format) {
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
                throw new IllegalArgumentException("File format is not supported: " + format);
        }
        BufferedImage image;
        BufferedImageReader imageReader = new BufferedImageReader(reader);
        try {
            imageReader.setId(streamId);
            image = imageReader.openImage(0);
            imageReader.close();
        } catch (IOException | FormatException e) {
            throw new IllegalArgumentException("Invalid image stream id", e);
        }
        return image;
    }

    public static BufferedImage readImageFromLocalFile(String localFilePath) {
        String format = FilenameUtils.getExtension(localFilePath);
        String selectedRenderer = FrameworkAccess.getModelProperty(
                OptionConstants.DISPLAY_RENDERER_2D, RendererType2D.LOCI.toString());
        RendererType2D renderer = RendererType2D.valueOf(selectedRenderer);
        BufferedImage image;
        if (renderer == RendererType2D.IMAGE_IO) {
            InputStream imageStream;
            try {
                imageStream = new FileInputStream(localFilePath);
            } catch (IOException e) {
                throw new IllegalArgumentException("Cannot open local image file " + localFilePath, e);
            }
            try {
                image = readWithImageIOFromInputStream(imageStream);
            } catch (IOException e) {
                throw new IllegalArgumentException("Invalid local image file " + localFilePath, e);
            } finally {
                try {
                    imageStream.close();
                } catch (IOException ignore) {
                }
            }
        } else {
            image = readWithLociReaderFromStreamId(localFilePath, format);
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
                FileProxy fileProxy = FileMgr.getFileMgr().getFile(filePath, false);
                file = fileProxy.getLocalFile();
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
                FrameworkAccess.handleException(error);
            }
        };
        worker.setProgressMonitor(new IndeterminateProgressMonitor(FrameworkAccess.getMainFrame(), "Retrieving file...", ""));
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
                FrameworkAccess.handleException(e);
            }
        }
        else {
            Utils.cacheAndProcessFileAsync(filePath, new FileCallable() {
                @Override
                public void call(File file) throws Exception {
                    if (file == null) {
                        JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                                "Could not open file path", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    else {
                        callback.call(file);
                    }
                }
            });
        }
    }

    public static void copyURLToFile(String standardPath, File destination, SimpleWorker worker, boolean hasProgress) throws Exception {

        if (worker != null) {
            worker.throwExceptionIfCancelled();
        }
        
        log.trace("copyURLToFile: standardPath={}, destination={}", standardPath, destination);

        final File destinationDir = destination.getParentFile();
        if ((destinationDir != null) && (! destinationDir.exists())) {
            Files.createDirectories(destinationDir.toPath());
        }

        //make sure we can write to destination
        if (destination.exists() && !destination.canWrite()) {
            throw new IOException("Unable to open " + destination.getAbsolutePath() + " for writing.");
        }

        InputStream input;
        FileProxy fileProxy = FileMgr.getFileMgr().getFile(standardPath, false);

        Long length;
        log.info("copyURLToFile {} to {}", standardPath, destination);

        int estimatedCompressionFactor;
        InputStream fileProxyStream = fileProxy.openContentStream();
        try {
            if (standardPath.endsWith(EXTENSION_BZ2) &&
                    (!destination.getName().endsWith(EXTENSION_BZ2))) {
                input = new BZip2CompressorInputStream(fileProxyStream, true);
                estimatedCompressionFactor = 3;
                length = null;
            } else {
                input = fileProxyStream;
                estimatedCompressionFactor = 1;
                length = fileProxy.estimateSizeInBytes();
            }
        } catch (Exception e) {
            IOUtils.closeQuietly(fileProxyStream);
            throw e;
        }

        FileOutputStream output = new FileOutputStream(destination);
        try {
            final long totalBytesWritten = copy(input, output, length, worker, estimatedCompressionFactor, hasProgress);
            if (length != null && totalBytesWritten < length) {
                throw new CancellationException("Bytes written (" + totalBytesWritten + ") for " + fileProxy.getFileId() +
                                      " is less than source length (" + length + ")");
            }
        } finally {
            IOUtils.closeQuietly(input); // close input here to ensure bzip stream is properly closed
            IOUtils.closeQuietly(output);
        }
    }

    public static void copyFileToFile(File source, File destination, SimpleWorker worker, boolean hasProgress) throws Exception {


        if (worker != null) {
            worker.throwExceptionIfCancelled();
        }
        
        log.trace("copyFileToFile: source={}, destination={}", source, destination);

        final File destinationDir = destination.getParentFile();
        if ((destinationDir != null) && (! destinationDir.exists())) {
            Files.createDirectories(destinationDir.toPath());
        }

        // make sure we can write to destination
        if (destination.exists() && !destination.canWrite()) {
            throw new IOException("Unable to open " + destination.getAbsolutePath() + " for writing.");
        }
        
        InputStream input = null;
        FileOutputStream output = null;

        try {
            input = new FileInputStream(source);
            Long length = source.length();
            log.info("copyURLToFile: length={}, source={}", length, source);
    
            int estimatedCompressionFactor = 1;
            if (source.getName().endsWith(EXTENSION_BZ2) &&
                    (! destination.getName().endsWith(EXTENSION_BZ2))) {
                input = new BZip2CompressorInputStream(input, true);
                estimatedCompressionFactor = 3;
                length = null;
            }
    
            output = new FileOutputStream(destination);
            
            final long totalBytesWritten = copy(input, output, length, worker, estimatedCompressionFactor, hasProgress);
            if (totalBytesWritten < source.length()) {
                throw new CancellationException("Bytes written (" + totalBytesWritten + ") for " + destination +
                                      " is less than source length (" + length + ")");
            }
        } 
        finally {
            if (input!=null) {
                IOUtils.closeQuietly(input); // close input here to ensure bzip stream is properly closed
            }
            if (output!=null) {
                IOUtils.closeQuietly(output);
            }
        }
    }

    /**
     * Adapted from Apache's commons-io, so that we could add progress percentage and status.
     */
    private static long copy(InputStream input, FileOutputStream output, Long length,
                             SimpleWorker worker, int estimatedCompressionFactor, 
                             boolean hasProgress) throws IOException {

        BackgroundWorker backgroundWorker = null;
        String backgroundStatus = null;
        if (hasProgress && worker!=null) {
            if (worker instanceof BackgroundWorker) {
                backgroundWorker = (BackgroundWorker) worker;
                backgroundStatus = backgroundWorker.getStatus();
                if (backgroundStatus == null) {
                    backgroundStatus = "Copying file - ";
                } else {
                    backgroundStatus = backgroundStatus + " - ";
                }
            }
        }
        final BackgroundWorker finalBackgroundWorker = backgroundWorker;
        final String finalBackgroundStatus = backgroundStatus;

        final long startTime = System.currentTimeMillis();
        final long estimatedLength = length == null ? 1 : estimatedCompressionFactor * length;
        long totalBytesWritten = 0;
        
        if (length != null) {
            // 30 MB/s on Windows (Windows to NAS)
            CallbackByteChannel rbc = new CallbackByteChannel(Channels.newChannel(input), (long bytesWritten) -> {
                worker.setProgress(bytesWritten, estimatedLength);
                if (finalBackgroundWorker != null) {
                    final long elapsedTime = System.currentTimeMillis() - startTime;
                    TransferSpeed speed = new TransferSpeed(elapsedTime, bytesWritten);
                    String message = String.format("Wrote %.2f %s (%.2f MB/s)", 
                            speed.getAmountWritten(), speed.getAmountUnits(), speed.getMbps());
                    finalBackgroundWorker.setStatus(finalBackgroundStatus + message);
                }
            });
            output.getChannel().transferFrom(rbc, 0, length);
            totalBytesWritten = rbc.getTotalBytesRead();
        }
        else {
            log.warn("No length given, falling back on inefficient copy method");

            // 10-15 MB/s (Windows to NAS)
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int bytesRead;
            long totalBytesWrittenAtLastStatusUpdate = totalBytesWritten;
            while (-1 != (bytesRead = input.read(buffer))) {
    
                output.write(buffer, 0, bytesRead);
                totalBytesWritten += bytesRead;
    
                if (worker != null) {
                    worker.throwExceptionIfCancelled();
    
                    if (hasProgress) {
                        if ((totalBytesWritten - totalBytesWrittenAtLastStatusUpdate) > TEN_MEGABYTES) {
       
                            totalBytesWrittenAtLastStatusUpdate = totalBytesWritten;
    
                            if (totalBytesWritten < estimatedLength) {
                                worker.setProgress(totalBytesWritten, estimatedLength);
                            }
        
                            if (backgroundWorker != null) {
                                final long elapsedTime = System.currentTimeMillis() - startTime;
                                TransferSpeed speed = new TransferSpeed(elapsedTime, totalBytesWritten);
                                String message = String.format("Wrote %.2f %s (%.2f MB/s)", 
                                        speed.getAmountWritten(), speed.getAmountUnits(), speed.getMbps());
                                finalBackgroundWorker.setStatus(finalBackgroundStatus + message);
                            }
                        }
                    }
                }
            }
        }

        if (log.isInfoEnabled()) {
            final long elapsedTime = System.currentTimeMillis() - startTime;
            TransferSpeed speed = new TransferSpeed(elapsedTime, totalBytesWritten);
            String message = String.format("Wrote %.2f %s in %.2f seconds (%.2f MB/s)", 
                    speed.getAmountWritten(), speed.getAmountUnits(), speed.getElapsedSeconds(), speed.getMbps());
            log.info(message);
            if (backgroundWorker != null) {
                backgroundWorker.setStatus(message);
            }
        }

        return totalBytesWritten;
    }

    private static class TransferSpeed {
        
        private BigDecimal mbps;
        private BigDecimal amountWritten;
        private String amountUnits;
        private BigDecimal elapsedSeconds;

        public TransferSpeed(long elapsedTime, long totalBytesWritten) {

            elapsedSeconds = divideAndScale(elapsedTime, 1000, 1);
            if (totalBytesWritten > ONE_GIGABYTE) {
                amountWritten = divideAndScale(totalBytesWritten, ONE_GIGABYTE, 1);
                amountUnits = "GB";
            }
            else if (totalBytesWritten > ONE_MEGABYTE) {
                amountWritten = divideAndScale(totalBytesWritten, ONE_MEGABYTE, 1);
                amountUnits = "MB";
            } 
            else {
                amountWritten = divideAndScale(totalBytesWritten, ONE_KILOBYTE, 1);
                amountUnits = "KB";
            }
            
            BigDecimal mbWritten = divideAndScale(totalBytesWritten, ONE_MEGABYTE, 1);
            mbps = elapsedSeconds.intValue()==0 ? mbWritten : mbWritten.divide(elapsedSeconds, 2, RoundingMode.HALF_UP);
        }
        
        public BigDecimal getElapsedSeconds() {
            return elapsedSeconds;
        }

        public BigDecimal getAmountWritten() {
            return amountWritten;
        }

        public String getAmountUnits() {
            return amountUnits;
        }

        public BigDecimal getMbps() {
            return mbps;
        }
    }
    
    private static BigDecimal divideAndScale(double numerator, double denominator, int scale) {
        return new BigDecimal(numerator / denominator).setScale(scale, BigDecimal.ROUND_HALF_UP);
    }

    public static void openUrlInBrowser(String url) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(URI.create(url));
            }
            catch (IOException ex) {
                log.error("Could not open URL: "+url, ex);
            }
        }
    }
    
    public static boolean replaceInFile(String filepath, String target, String replacement) throws IOException {
        Path path = Paths.get(filepath);
        Charset charset = StandardCharsets.UTF_8;
        String content = new String(Files.readAllBytes(path), charset);
        String fixed = content.replace(target, replacement);
        if (!content.equals(fixed)) {
            Files.write(path, fixed.getBytes(charset));
            return true;
        }
        return false;
    }

    // From https://stackoverflow.com/questions/1555262/calculating-the-difference-between-two-java-date-instances
    public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
        long diffInMillies = date2.getTime() - date1.getTime();
        return timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS);
    }
    
    public static File getOutputFile(String dir, String prefix, String extension) {
        File defaultFile = new File(dir, prefix+"."+extension);

        int i = 1;
        while (defaultFile.exists() && i < 100000) {
            defaultFile = new File(dir, prefix + "_" + i + "." + extension);
            i++;
        }
        
        if (defaultFile.exists()) {
            throw new RuntimeException("Could not create file, "+defaultFile+" already exists.");
        }
        
        return defaultFile;
    }

    public static void setDownloadsDir(String downloadsDir) {
        FrameworkAccess.setModelProperty(OptionConstants.FILE_DOWNLOADS_DIR, downloadsDir);
    }

    public static Path getDownloadsDir() {

        String fileDownloadsDir = (String) FrameworkAccess.getModelProperty(OptionConstants.FILE_DOWNLOADS_DIR);

        Path fileDownloadsPath;
        // Check for existence and clear out references to tmp
        if (fileDownloadsDir==null || fileDownloadsDir.startsWith("/tmp")) {
            Path downloadDir = Paths.get(System.getProperty(SystemInfo.USERHOME_SYSPROP_NAME), SystemInfo.DOWNLOADS_DIR);
            fileDownloadsPath = downloadDir.resolve(SystemInfo.WORKSTATION_FILES_DIR);
        }
        else {
            fileDownloadsPath = Paths.get(fileDownloadsDir);
        }

        try {
            if (!Files.exists(fileDownloadsPath)) {
                Files.createDirectories(fileDownloadsPath);
                log.debug("Created download dir: "+fileDownloadsPath.toString());
            }
        }
        catch (Exception e) {
            log.error("Error trying to test and create a download directory", e);
        }

        return fileDownloadsPath;
    }

    /**
     * Gets the -Xmx setting in current use.
     *
     * @return gigs being requested at launch.
     */
    public static Integer getMemoryAllocation() {
        return BrandingConfig.getBrandingConfig().getMemoryAllocationGB();
    }

    /**
     * Sets the ultimate -Xmx allocation setting.
     * @param memoryInGb how many gigs to use.
     */
    public static void setMemoryAllocation(Integer memoryInGb) throws IOException {
        BrandingConfig.getBrandingConfig().setMemoryAllocationGB(memoryInGb);
    }

}
