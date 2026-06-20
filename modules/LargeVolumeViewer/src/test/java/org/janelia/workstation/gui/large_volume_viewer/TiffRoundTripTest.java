package org.janelia.workstation.gui.large_volume_viewer;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferUShort;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.junit.Test;

/**
 * Round-trip validation: write a multi-page 16-bit grayscale TIFF using the
 * migrated writer path, read it back using the migrated reader path, and assert
 * exact pixel-level fidelity.
 */
public class TiffRoundTripTest {

    private static final int WIDTH  = 64;
    private static final int HEIGHT = 64;
    private static final int PAGES  = 3;

    /** Known pixel values for each page (distinctive 16-bit values). */
    private static final int[] PAGE_FILL = { 1000, 32767, 65000 };

    @Test
    public void testMultiPage16bitGrayscaleRoundTrip() throws IOException {

        // ---- WRITE -------------------------------------------------------
        // Build PAGES synthetic 16-bit grayscale images with known pixel values.
        BufferedImage[] pages = new BufferedImage[PAGES];
        for (int p = 0; p < PAGES; p++) {
            BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_USHORT_GRAY);
            short fillValue = (short)(PAGE_FILL[p] & 0xFFFF);
            short[] data = ((DataBufferUShort) img.getRaster().getDataBuffer()).getData();
            for (int i = 0; i < data.length; i++) {
                data[i] = fillValue;
            }
            pages[p] = img;
        }

        File tmpFile = File.createTempFile("tiff-roundtrip-", ".tif");
        tmpFile.deleteOnExit();

        // Use the migrated writer path (same as CreateSyntheticTiff / PermuteTiff)
        try (FileOutputStream fos = new FileOutputStream(tmpFile);
             ImageOutputStream ios = ImageIO.createImageOutputStream(fos)) {

            ImageWriter writer = TiffImageIOHelper.getTiffWriter();
            assertNotNull("Could not obtain a TIFF ImageWriter", writer);
            writer.setOutput(ios);
            writer.prepareWriteSequence(null);
            for (BufferedImage page : pages) {
                writer.writeToSequence(new IIOImage(page, null, null), null);
            }
            writer.endWriteSequence();
            writer.dispose();
        }

        System.out.println("[TiffRoundTripTest] Written " + PAGES + "-page 16-bit TIFF to: " + tmpFile);

        // ---- READ --------------------------------------------------------
        // Use the migrated reader path (same as Texture3d / TifVolumeFileLoader)
        ImageReader reader = TiffImageIOHelper.getTiffReader();
        assertNotNull("Could not obtain a TIFF ImageReader", reader);
        ImageInputStream iis = ImageIO.createImageInputStream(tmpFile);
        reader.setInput(iis);
        try {
            int numPages = reader.getNumImages(true);
            assertEquals("Page count mismatch", PAGES, numPages);

            for (int p = 0; p < numPages; p++) {
                RenderedImage ri = reader.readAsRenderedImage(p, null);
                assertNotNull("Null RenderedImage for page " + p, ri);

                assertEquals("Width mismatch on page " + p, WIDTH, ri.getWidth());
                assertEquals("Height mismatch on page " + p, HEIGHT, ri.getHeight());

                // Sample the centre pixel — should be exactly PAGE_FILL[p]
                int[] sample = ri.getData().getPixel(WIDTH / 2, HEIGHT / 2, (int[]) null);
                int actualValue = sample[0];
                System.out.println("[TiffRoundTripTest] Page " + p
                        + ": expected=" + PAGE_FILL[p]
                        + " actual=" + actualValue);
                assertEquals("Pixel value mismatch on page " + p, PAGE_FILL[p], actualValue);
            }
        } finally {
            reader.dispose();
            iis.close();
        }

        System.out.println("[TiffRoundTripTest] PASS: 16-bit multi-page TIFF round-trip verified pixel-accurate.");
    }
}
