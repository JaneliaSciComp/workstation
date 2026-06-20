package org.janelia.workstation.gui.large_volume_viewer;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import java.util.Iterator;

/**
 * Helper for acquiring TIFF ImageReader/ImageWriter instances.
 * <p>
 * Under NetBeans module isolation, {@link ImageIO} SPI discovery may fail to find
 * TwelveMonkeys providers. This helper first tries the standard SPI route, then
 * falls back to instantiating the TwelveMonkeys SPIs directly.
 */
public final class TiffImageIOHelper {

    private TiffImageIOHelper() {}

    /**
     * Returns a TIFF {@link ImageReader}. Tries SPI discovery first; if that returns
     * nothing, falls back to the TwelveMonkeys {@code TIFFImageReaderSpi} directly.
     *
     * @return a ready-to-use TIFF ImageReader (caller must call {@code setInput})
     * @throws IllegalStateException if no TIFF reader is available
     */
    public static ImageReader getTiffReader() {
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("TIFF");
        if (readers.hasNext()) {
            return readers.next();
        }
        // Fallback: instantiate TwelveMonkeys SPI directly (avoids NetBeans classloader issues)
        try {
            return new com.twelvemonkeys.imageio.plugins.tiff.TIFFImageReaderSpi()
                    .createReaderInstance(null);
        } catch (Exception e) {
            throw new IllegalStateException("No TIFF ImageReader available", e);
        }
    }

    /**
     * Returns a TIFF {@link ImageWriter}. Tries SPI discovery first; if that returns
     * nothing, falls back to the TwelveMonkeys {@code TIFFImageWriterSpi} directly.
     *
     * @return a ready-to-use TIFF ImageWriter (caller must call {@code setOutput})
     * @throws IllegalStateException if no TIFF writer is available
     */
    public static ImageWriter getTiffWriter() {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("TIFF");
        if (writers.hasNext()) {
            return writers.next();
        }
        // Fallback: instantiate TwelveMonkeys SPI directly
        try {
            return new com.twelvemonkeys.imageio.plugins.tiff.TIFFImageWriterSpi()
                    .createWriterInstance(null);
        } catch (Exception e) {
            throw new IllegalStateException("No TIFF ImageWriter available", e);
        }
    }
}
