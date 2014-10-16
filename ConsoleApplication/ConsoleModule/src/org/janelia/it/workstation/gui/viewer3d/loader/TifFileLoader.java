package org.janelia.it.workstation.gui.viewer3d.loader;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.MemoryCacheSeekableStream;
import com.sun.media.jai.codec.SeekableStream;
import com.sun.media.jai.codec.TIFFDecodeParam;
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
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.media.jai.NullOpImage;
import javax.media.jai.OpImage;
import javax.media.jai.RenderedImageAdapter;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataBean;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataI;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 2/6/13
 * Time: 3:33 PM
 *
 * Handles TIFF via Loci reading capability.
 */
public class TifFileLoader extends TextureDataBuilder implements VolumeFileLoaderI {
    
    private int sheetCountFromFile = -1;
    
    private LoaderSubsetHelper subsetHelper;
    
    /**
     * Sets maximum size in all dimensions, to add to outgoing image.
     * 
     * @param cubicOutputDimension how many voxels to use.
     */
    public void setCubicOutputDimension( int cubicOutputDimension ) {
        if ( subsetHelper == null )
            subsetHelper = new LoaderSubsetHelper();
        subsetHelper.setCubicOutputDimension(cubicOutputDimension);
    }
    
    /**
     * Tell camera proximity for sake of bounding box calculation.
     * @param distance 
     */
    public void setCameraToCentroidDistance( int[] distance ) {
        if ( subsetHelper == null )
            subsetHelper = new LoaderSubsetHelper();
        subsetHelper.setCameraToCentroidDistance(distance);
    }
    
    public void setConversionCharacteristics( double[][] transform, int[] minCorner, int[] extent, int[] queryCoords ) {
        if ( subsetHelper == null ) {
            subsetHelper = new LoaderSubsetHelper();            
        }
        subsetHelper.setTransformCharacteristics(transform, minCorner, extent, queryCoords);
    }
    
    @Override
    public TextureDataI createTextureDataBean() {
        TextureDataBean textureDataBean;
        if ( pixelBytes < 4  ||  subsetHelper != null ) {
            textureDataBean = new TextureDataBean( textureByteArray, sx, sy, sz );
        }
        else {
            textureDataBean = new TextureDataBean( argbTextureIntArray, sx, sy, sz );
        }
        textureDataBean.setPixelByteCount(pixelBytes);
        return textureDataBean;
    }

    @Override
    public void loadVolumeFile( String fileName ) throws Exception {
        this.unCachedFileName = fileName;
        sx = -1;
        
        final File file = new File(fileName);
        Collection<BufferedImage> allImages = loadTIFF( file );
        pixelBytes = -1;
        int zOffset = 0;
        int sheetSize = -1;
        int targetOffset = 0;
        int expectedWidth = 0;
        int expectedHeight = 0;
        for ( BufferedImage zSlice: allImages ) {
            if ( sx == -1 ) {
                sx = zSlice.getWidth();
                sy = zSlice.getHeight();
                sz = allImages.size();
                if ( subsetHelper != null ) {
                    subsetHelper.setSx(zSlice.getWidth());
                    subsetHelper.setSy(zSlice.getHeight());
                    subsetHelper.setSourceWidth(sx);
                    subsetHelper.setSourceHeight(sy);
                    sheetSize = subsetHelper.captureAndUsePageDimensions(allImages.size(), file.length());
                    sx = subsetHelper.getSx();
                    sy = subsetHelper.getSy();
                    sz = subsetHelper.getSz();
                    pixelBytes = subsetHelper.getPixelBytes();
                    argbTextureIntArray = subsetHelper.getArgbTextureIntArray();
                    textureByteArray = subsetHelper.getTextureByteArray();
                    expectedWidth = subsetHelper.getSourceWidth();
                    expectedHeight = subsetHelper.getSourceHeight();
                }
                else {
                    sheetSize = captureAndUsePageDimensions( allImages.size(), file.length() );
                    expectedWidth = sx;
                    expectedHeight = sy;
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
            else if (subsetHelper.inZSubset( zOffset )) {
                subsetHelper.storeSubsetToBuffer(targetOffset++, sheetSize, zSlice);
            }
            zOffset ++;
        }
    }
    
    public int captureAndUsePageDimensions(final int zCount, final long fileLength) {
        pixelBytes = (int)Math.floor( fileLength / ((sx*sy) * sz) );
        if ( pixelBytes == 4 ) {
            argbTextureIntArray = new int[ sx * sy * sz ];
        }
        else {
            textureByteArray = new byte[ sx * sy * sz * pixelBytes ];
        }
        return sx * sy;
    }
    
    private void storeToBuffer(int zOffset, int sheetSize, BufferedImage zSlice) {
        final int outputBufferOffset = zOffset * sheetSize;
        if ( pixelBytes == 1 ) {
            DataBufferByte db = ((DataBufferByte)zSlice.getTile(0, 0).getDataBuffer());
            byte[] pixels = db.getData();
            System.arraycopy(pixels, 0, textureByteArray, outputBufferOffset, sheetSize);
        }
        else if ( pixelBytes == 2 ) {
            DataBufferUShort db = ((DataBufferUShort)zSlice.getTile(0, 0).getDataBuffer());
            short[] pixels = db.getData();
            int shortOffset = pixelBytes * outputBufferOffset;
            for ( int i = 0; i < pixels.length; i++ ) {
                // Changing the order.
                int unsignedPixelVal = pixels[ i ];
                if ( pixels[ i ] < 0 ) {
                    unsignedPixelVal += 65536;
                }
                byte byteVal = (byte)((unsignedPixelVal & 0x0000ff00) >> 8);
                textureByteArray[ i * pixelBytes + shortOffset + 1 ] = byteVal;
                byteVal = (byte)(unsignedPixelVal & 0x000000ff);
                textureByteArray[ i * pixelBytes + shortOffset ] = byteVal;
            }
        }
        else if ( pixelBytes == 4 ) {
            zSlice.getRGB(0, 0,
                    sx, sy,
                    argbTextureIntArray, outputBufferOffset,
                    sx);
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
            if ( subsetHelper != null )
                subsetHelper.setSourceDepth( sheetCountFromFile );
            
            for (int imageToLoad = 0; imageToLoad < maxPage; imageToLoad++) {
                RenderedImage op
                        = new NullOpImage(dec.decodeAsRenderedImage(imageToLoad),
                                null,
                                OpImage.OP_IO_BOUND,
                                null);

                wholeImage = renderedToBuffered(op);

                imageCollection.add(wholeImage);
            }
            
            return imageCollection;


        } catch (IOException e) {
            System.out.println(e.toString());

            return null;
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
    private Collection<BufferedImage> loadTIFFParallel(File file) {
        final Collection<BufferedImage> imageCollection = Collections.<BufferedImage>synchronizedCollection(new ArrayList<BufferedImage>());
        try {
            byte[] bytes = readBytes(file);
            
            SeekableStream s = new MemoryCacheSeekableStream( new ByteArrayInputStream( bytes ) );
            TIFFDecodeParam param = null;
            ImageDecoder dec = ImageCodec.createImageDecoder("tiff", s, param);
            final int numPages = dec.getNumPages();

            //NOTE: level of redundancy, here, suggests, may not be saving any
            // real time doing this load.
            final Collection<ImageLoadRunnable> runnables = new ArrayList<>();
            ExecutorService executorService = Executors.newFixedThreadPool( 20 );           
            for (int imageToLoad = 0; imageToLoad < numPages; imageToLoad++) {
                SeekableStream pageS = new MemoryCacheSeekableStream( new ByteArrayInputStream( bytes ) );
                ImageDecoder pageDec = ImageCodec.createImageDecoder("tiff", pageS, null);
                ImageLoadRunnable runnable = new ImageLoadRunnable( imageToLoad, pageDec, imageCollection, TifFileLoader.this );
                runnables.add( runnable );
                executorService.execute( runnable );
            }
            
            executorService.shutdown();
            try {
                executorService.awaitTermination( 10, TimeUnit.MINUTES );
            } catch ( InterruptedException ie ) {
                throw new RuntimeException( "Interrupted while awaiting complestion of load of " + file, ie );
            }
            
            for ( ImageLoadRunnable runnable: runnables ) {
                if ( runnable.getThrownException() != null ) {
                    throw new RuntimeException(
                      "One or more pages from Tiff " + file + " failed to load."
                    );
                }
            }
            
            return imageCollection;


        } catch (IOException e) {
            System.out.println(e.toString());

            return null;
        }

    }

    private byte[] readBytes(File file) throws RuntimeException {
        byte[] bytes = new byte[ (int)file.length() ];
        try (FileInputStream fis = new FileInputStream( file )) {
            fis.read(bytes);
        } catch ( IOException ioe ) {
            throw new RuntimeException(ioe);
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
    
    private static class ImageLoadRunnable implements Runnable {
        private Exception thrownException;
        private int imageToLoad;
        private ImageDecoder dec;
        private Collection<BufferedImage> imageCollection;
        private TifFileLoader loader;
        
        public ImageLoadRunnable(int imageToLoad, ImageDecoder dec, Collection<BufferedImage> imageCollection, TifFileLoader loader) {
            this.imageToLoad = imageToLoad;
            this.dec = dec;
            this.imageCollection = imageCollection;
            this.loader = loader;
        }
        
        @Override
        public void run() {
            try {
                loader.getImage( dec, imageToLoad, imageCollection );
            } catch (IOException ex) {
                this.thrownException = ex;
            }
        }

        /**
         * @return the thrownException
         */
        public Exception getThrownException() {
            return thrownException;
        }

    }
        
}
