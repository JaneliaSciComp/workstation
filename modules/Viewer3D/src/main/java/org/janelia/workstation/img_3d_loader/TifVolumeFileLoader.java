/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.workstation.img_3d_loader;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.log4j.Logger;
import org.janelia.workstation.img_3d_loader.AbstractVolumeFileLoader;
import org.janelia.workstation.img_3d_loader.LoaderSubsetHelper;

/**
 * Pull Tif file into memory.
 * @author fosterl
 */
public class TifVolumeFileLoader extends AbstractVolumeFileLoader {
    public static final int SENTINAL_INT_VAL = -1;

    private LoaderSubsetHelper subsetHelper;
    private int sheetCountFromFile;

    private static final Logger logger = Logger.getLogger(TifVolumeFileLoader.class);
    public static final int LOAD_SIZE = 8 * 1024 * 1024;

    /**
     * Sets maximum size in all dimensions, to add to outgoing image.
     *
     * @param dimensions how many voxels to use.
     */
    public void setOutputDimensions( int[] dimensions ) {
        if ( subsetHelper == null ) {
            subsetHelper = new LoaderSubsetHelper();
        }
        subsetHelper.setOutputDimensions(dimensions);
    }

    public void setConversionCharacteristics( double[][] fwdTransform, double[][] invTransform, int[] minCorner, int[] extent, List<Integer> queryCoords ) {
        if ( subsetHelper == null ) {
            subsetHelper = new LoaderSubsetHelper();
        }
        subsetHelper.setTransformCharacteristics(fwdTransform, invTransform, minCorner, extent, queryCoords);
    }

    @Override
    public void loadVolumeFile( String fileName ) throws Exception {
        setUnCachedFileName(fileName);

        final File file = new File(fileName);
        logger.debug("Loading the subset of images.");
        Collection<BufferedImage> allImages = loadTIFF( file );
        if ( allImages == null ) {
            throw new Exception("Failed to read data from " + fileName + ".");
        }

        // Sentinal values.
        setSx(SENTINAL_INT_VAL);
        setSy(SENTINAL_INT_VAL);
        setSz(SENTINAL_INT_VAL);
        setPixelBytes(SENTINAL_INT_VAL);
        int sheetSize = SENTINAL_INT_VAL;
        int expectedWidth = SENTINAL_INT_VAL;
        int expectedHeight = SENTINAL_INT_VAL;

        logger.debug("Traversing images.");
        // Initial values.
        int zOffset = 0;
        int targetOffset = 0;
        for ( BufferedImage zSlice: allImages ) {
            if ( expectedWidth == SENTINAL_INT_VAL ) {
                expectedWidth = zSlice.getWidth();
                expectedHeight = zSlice.getHeight();
            }
            if ( getSy() == SENTINAL_INT_VAL ) {
                if ( subsetHelper != null ) {
                    subsetHelper.setSourceWidth(zSlice.getWidth());
                    subsetHelper.setSourceHeight(zSlice.getHeight());
                    subsetHelper.calculateBoundingBox(sheetCountFromFile);

                    // Apply volume characteristics 'learned' from subset helper.
                    setSx(subsetHelper.getSx());
                    setSy(subsetHelper.getSy());
                    setSz(subsetHelper.getSz());

                    sheetSize = subsetHelper.initializeStorage(file.length());
                    setPixelBytes( subsetHelper.getPixelBytes() );
                    setArgbTextureIntArray(subsetHelper.getArgbTextureIntArray());
                    setTextureByteArray(subsetHelper.getTextureByteArray());
                }
                else {
                    setSx( zSlice.getWidth() );
                    setSy( zSlice.getHeight() );
                    setSz( allImages.size() );
                    sheetSize = initializeStorage(file.length());
                }
            }
            else {
                if ( expectedWidth != zSlice.getWidth()  ||  expectedHeight != zSlice.getHeight() ) {
                    throw new IllegalStateException( "Image number " + zOffset +
                            " with HEIGHT=" + zSlice.getHeight() + " and WIDTH=" +
                            zSlice.getWidth() + " has dimensions which do not match previous width * height of " + expectedWidth + " * " + expectedHeight );
                }
            }

            // Store only things that are within the targetted depth.
            if ( subsetHelper == null ) {
                storeToBuffer(targetOffset++, sheetSize, zSlice);
            }
            else {
                subsetHelper.storeSubsetToBuffer(targetOffset++, sheetSize, zSlice);
            }
            zOffset ++;
        }
    }

    public int initializeStorage(final long fileLength) {
        setPixelBytes((int)Math.floor( fileLength / ((getSx()*getSy()) * getSz()) ));
        if ( getPixelBytes() == 4 ) {
            setArgbTextureIntArray(new int[ getSx() * getSy() * getSz() ]);
        }
        else {
            setTextureByteArray(new byte[ getSx() * getSy() * getSz() * getPixelBytes() ]);
        }
        return getSx() * getSy();
    }

    private void storeToBuffer(int zOffset, int sheetSize, BufferedImage zSlice) {
        final int outputBufferOffset = zOffset * sheetSize;
        if ( getPixelBytes() == 1 ) {
            DataBufferByte db = ((DataBufferByte)zSlice.getTile(0, 0).getDataBuffer());
            byte[] pixels = db.getData();
            System.arraycopy(pixels, 0, getTextureByteArray(), outputBufferOffset, sheetSize);
        }
        else if ( getPixelBytes() == 2 ) {
            DataBufferUShort db = ((DataBufferUShort)zSlice.getTile(0, 0).getDataBuffer());
            short[] pixels = db.getData();
            int shortOffset = getPixelBytes() * outputBufferOffset;
            for ( int i = 0; i < pixels.length; i++ ) {
                // Changing the order.
                int unsignedPixelVal = pixels[ i ];
                if ( pixels[ i ] < 0 ) {
                    unsignedPixelVal += 65536;
                }
                byte byteVal = (byte)((unsignedPixelVal & 0x0000ff00) >> 8);
                getTextureByteArray()[ i * getPixelBytes() + shortOffset + 1 ] = byteVal;
                byteVal = (byte)(unsignedPixelVal & 0x000000ff);
                getTextureByteArray()[ i * getPixelBytes() + shortOffset ] = byteVal;
            }
        }
        else if ( getPixelBytes() == 4 ) {
            zSlice.getRGB(0, 0,
                    getSx(), getSy(),
                    getArgbTextureIntArray(), outputBufferOffset,
                    getSx());
        }
    }

    /**
     * Load specified tiff pages and return as buffered images.
     *
     * @param file TIFF file to load
     * @return collection of BufferedImages, one per page
     */
    private Collection<BufferedImage> loadTIFF(File file) {
        Collection<BufferedImage> imageCollection = new ArrayList<>();
        ImageReader reader = TiffImageIOHelper.getTiffReader();
        try {
            ImageInputStream iis = ImageIO.createImageInputStream(file);
            reader.setInput(iis);
            logger.debug("In loadTIFF " + file + " getting number of pages...");
            int maxPage = reader.getNumImages(true);
            sheetCountFromFile = maxPage;
            if ( subsetHelper != null ) {
                subsetHelper.setSourceDepth( sheetCountFromFile );
                subsetHelper.calculateBoundingZ( sheetCountFromFile );
            }

            if ( logger.isDebugEnabled() )
                logger.debug("In loadTIFF " + file + " reading pages loop.");
            for (int imageToLoad = 0; imageToLoad < maxPage; imageToLoad++) {
                if ( subsetHelper == null  ||  subsetHelper.inZSubset( imageToLoad ) ) {
                    RenderedImage ri = reader.readAsRenderedImage(imageToLoad, null);
                    BufferedImage wholeImage = renderedToBuffered(ri);
                    imageCollection.add(wholeImage);
                }
                if ( logger.isDebugEnabled() )
                    logger.debug("In loadTIFF " + file + " page completed: " + imageToLoad);
            }
            logger.debug("In loadTIFF " + file + " returning image collection.");
            iis.close();
            return imageCollection;

        } catch (IOException e) {
            logger.error(e.toString());
            return null;
        } finally {
            reader.dispose();
        }
    }

    /**
     * Convert RenderedImage to BufferedImage, preserving sample model (e.g. 16-bit grayscale).
     *
     * @param img source image
     * @return BufferedImage with the same pixel data
     */
    private static BufferedImage renderedToBuffered(RenderedImage img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }
        // Copy to a new BufferedImage preserving the color model and sample model
        BufferedImage bi = new BufferedImage(
                img.getColorModel(),
                img.getColorModel().createCompatibleWritableRaster(img.getWidth(), img.getHeight()),
                img.getColorModel().isAlphaPremultiplied(),
                null);
        Graphics2D g = bi.createGraphics();
        g.drawRenderedImage(img, new java.awt.geom.AffineTransform());
        g.dispose();
        return bi;
    }

    private void getImage( ImageReader reader, int imageToLoad, Collection<BufferedImage> imageCollection ) throws IOException {
        RenderedImage ri = reader.readAsRenderedImage(imageToLoad, null);
        BufferedImage wholeImage = renderedToBuffered(ri);
        imageCollection.add(wholeImage);
    }

}
