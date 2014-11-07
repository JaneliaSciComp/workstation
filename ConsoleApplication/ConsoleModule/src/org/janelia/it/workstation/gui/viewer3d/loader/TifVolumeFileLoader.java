/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.viewer3d.loader;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.MemoryCacheSeekableStream;
import com.sun.media.jai.codec.SeekableStream;
import com.sun.media.jai.codec.TIFFDecodeParam;

import javax.media.jai.NullOpImage;
import javax.media.jai.OpImage;
import javax.media.jai.RenderedImageAdapter;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.awt.image.RenderedImage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Pull Tif file into memory.
 * @author fosterl
 */
public class TifVolumeFileLoader extends AbstractVolumeFileLoader {
    public static final int SENTINAL_INT_VAL = -1;

    private LoaderSubsetHelper subsetHelper;
    private int sheetCountFromFile;
    
    private static final Logger logger = Logger.getLogger(TifVolumeFileLoader.class);

    /**
     * Sets maximum size in all dimensions, to add to outgoing image.
     * 
     * @param cubicOutputDimension how many voxels to use.
     */
    public void setCubicOutputDimension( int cubicOutputDimension ) {
        if ( subsetHelper == null ) {
            subsetHelper = new LoaderSubsetHelper();
        }
        subsetHelper.setCubicOutputDimension(cubicOutputDimension);
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
                    sheetSize = captureAndUsePageDimensions( allImages.size(), file.length() );
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
    
    public int captureAndUsePageDimensions(final int zCount, final long fileLength) {
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
     * Load specified tiff page and return as buffered zSlice.
     * From: http://opencapture.googlecode.com/svn/0.0.2/OpenCapture/src/net/filterlogic/util/imaging/ToTIFF.java
     * 
     * @param file
     * @param imageToLoad Page to load
     * @return BufferedImage
     */
    private Collection<BufferedImage> loadTIFF(File file) {
        Collection<BufferedImage> imageCollection = new ArrayList<>();
        try {
            BufferedImage wholeImage = null;

            byte[] bytes = readBytes(file);            
            SeekableStream s = new MemoryCacheSeekableStream( new ByteArrayInputStream( bytes ) );
            //SeekableStream s = new FileSeekableStream(file);
            
            TIFFDecodeParam param = null;
            ImageDecoder dec = ImageCodec.createImageDecoder("tiff", s, param);
            int maxPage = dec.getNumPages();
            sheetCountFromFile = maxPage;            
            if ( subsetHelper != null ) {
                subsetHelper.setSourceDepth( sheetCountFromFile );
                subsetHelper.calculateBoundingZ( sheetCountFromFile );
            }
            
            for (int imageToLoad = 0; imageToLoad < maxPage; imageToLoad++) {
                if ( subsetHelper == null  ||  subsetHelper.inZSubset( imageToLoad ) ) {
                    RenderedImage op
                        = new NullOpImage(dec.decodeAsRenderedImage(imageToLoad),
                                null,
                                OpImage.OP_IO_BOUND,
                                null);
                    wholeImage = renderedToBuffered(op);
                    imageCollection.add(wholeImage);
                }
            }
            
            return imageCollection;


        } catch (IOException e) {
            logger.error(e.toString());

            return null;
        }

    }    

    private byte[] readBytes(File file) throws IOException {
        byte[] bytes = new byte[ (int)file.length() ];
        try (FileInputStream fis = new FileInputStream( file )) {
            fis.read(bytes);
        }
        return bytes;
    }
            
    /**
     * Convert RenderedImage to BufferedImage
     * @param img
     * @return BufferedImage
     */
    private static BufferedImage renderedToBuffered(RenderedImage img) {
        if (img instanceof BufferedImage) 
        {
            return (BufferedImage) img;
        }

        RenderedImageAdapter imageAdapter = new RenderedImageAdapter(img);
        BufferedImage bufImage = imageAdapter.getAsBufferedImage();
        return bufImage;
    }
    
    private void getImage( ImageDecoder dec, int imageToLoad, Collection<BufferedImage> imageCollection ) throws IOException {
        RenderedImage op
                = new NullOpImage(dec.decodeAsRenderedImage(imageToLoad),
                        null,
                        OpImage.OP_IO_BOUND,
                        null);

        BufferedImage wholeImage = renderedToBuffered(op);
        imageCollection.add(wholeImage);
    }

}
