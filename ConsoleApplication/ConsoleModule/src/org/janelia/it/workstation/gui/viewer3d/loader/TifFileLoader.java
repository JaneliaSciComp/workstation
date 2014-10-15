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

    private static final int SPACE_N = 3;
    private static final int START_X_INX = 0;
    private static final int END_X_INX = START_X_INX + SPACE_N;
    private static final int START_Y_INX = 1;
    private static final int END_Y_INX = START_Y_INX + SPACE_N;
    private static final int START_Z_INX = 2;
    private static final int END_Z_INX = START_Z_INX + SPACE_N;
    
    public static final int BOUNDARY_MULTIPLE = 4;
    private int[] boundingBox = new int[6];
    private int cubicOutputDimension = -1;
    private int[] cameraToCentroidDistance;
    private int sheetCountFromFile = -1;
    private int[] tempIntBuffer;
    
    private int sourceWidth;
    private int sourceHeight;
    
    /**
     * Sets maximum size in all dimensions, to add to outgoing image.
     * 
     * @param cubicOutputDimension how many voxels to use.
     */
    public void setCubicOutputDimension( int cubicOutputDimension ) {
        this.cubicOutputDimension = cubicOutputDimension;
    }
    
    public void setCameraToCentroidDistance( int[] distance ) {
        this.cameraToCentroidDistance = distance; 
    }
    
    @Override
    public TextureDataI createTextureDataBean() {
        TextureDataBean textureDataBean;
        if ( pixelBytes < 4  ||  cubicOutputDimension != -1 ) {
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
        int i = 2;
        for ( BufferedImage zSlice: allImages ) {
            if ( sx == -1 ) {
                sheetSize = captureAndUsePageDimensions(zSlice, allImages.size(), file);
            }
            else {
                if ( sourceWidth != zSlice.getWidth()  ||  sourceHeight != zSlice.getHeight() ) {
                    throw new IllegalStateException( "Image number " + zOffset +
                            " with HEIGHT=" + zSlice.getHeight() + " and WIDTH=" + 
                            zSlice.getWidth() + " has dimensions which do not match previous width * height of " + sourceWidth + " * " + sourceHeight );
                }
            }
            // Store only things that are within the targetted depth.
            if ( cubicOutputDimension == -1 ) {
                if ( tempIntBuffer == null ) {
                    tempIntBuffer = new int[ sourceWidth * sourceHeight ];
                }
                storeToBuffer(targetOffset++, sheetSize, zSlice);
            }
            else if (zOffset >= boundingBox[i] && zOffset < boundingBox[i+3]) {
                storeSubsetToBuffer(targetOffset++, sheetSize, zSlice);
            }
            zOffset ++;
        }
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
     * If this read is meant to take only a subset of the input and push that
     * to the output buffer, this method will carve out that chunk, and place
     * it into a buffer of the exact size needed.
     * 
     * @param destZ where in the output buffer to place this "sheet".
     * @param destSheetSize size of sheet: dest X * dest Y
     * @param zSlice a buffered image from which to extract a partial sheet.
     */
    private void storeSubsetToBuffer(int destZ, int destSheetSize, BufferedImage zSlice) {
        DataMover dataMover;
        if ( pixelBytes == 1 ) {
            DataBufferByte db = ((DataBufferByte)zSlice.getTile(0, 0).getDataBuffer());
            byte[] pixels = db.getData();
            dataMover = new ByteDataMover( pixels );
        }
        else if ( pixelBytes == 2 ) {
            DataBufferUShort db = ((DataBufferUShort)zSlice.getTile(0, 0).getDataBuffer());
            short[] pixels = db.getData();
            dataMover = new ShortDataMover( pixels );
        }
        else if ( pixelBytes == 4 ) {            
            zSlice.getRGB(0, 0,
                    sx, sy,
                    tempIntBuffer, 0,
                    sx);
            dataMover = new IntDataMover( tempIntBuffer );
        }
        else {
            throw new IllegalStateException( "Unexpected pixelBytes count == " + pixelBytes );
        }
        transferSubset(destZ, destSheetSize, dataMover);
    }

    /**
     * Convenience method encapsulating common functionality, to all widths
     * of data that will be transferred.
     * 
     * @param destZ how far along in output.
     * @param destSheetSize size of one x * y of data.
     * @param dataMover specific to pixel bytes count.
     */
    private void transferSubset(int destZ, int destSheetSize, DataMover dataMover) {
        // The source will be organized into one-voxel-per-array-entry.  That is
        // the source will be in bytes, words, ints, etc.
        int destOffset = ((destZ * destSheetSize) * pixelBytes);
        for ( int sourceY = boundingBox[ START_Y_INX ]; sourceY < boundingBox[ END_Y_INX ]; sourceY++ ) {
            int sourceOffset = (sourceWidth * sourceY) + boundingBox[ START_X_INX ];
            for ( int sourceX = boundingBox[ START_X_INX ]; sourceX < boundingBox[ END_X_INX ]; sourceX++ ) {
                // move pixelbytes worth of data from source to destination.
                dataMover.moveData(sourceOffset, destOffset);
                destOffset += pixelBytes;
                sourceOffset += pixelBytes;
            }
        }
        
    }

    private int captureAndUsePageDimensions(BufferedImage zSlice, final int zCount, final File file) {
        sx = zSlice.getWidth();
        sy = zSlice.getHeight();
        sourceWidth = sx;
        sourceHeight = sy;
        
        // Depth Limit is originally expressed as a part-stack size, based at 0.
        if ( cameraToCentroidDistance != null ) {
            // Z bounding box constraints are based on sheet depth.
            boundingBox[START_Z_INX] = clamp( 0, zCount - cubicOutputDimension, (zCount - cubicOutputDimension + cameraToCentroidDistance[START_Z_INX]) / 2 );
            boundingBox[END_Z_INX] = boundingBox[START_Z_INX] + cubicOutputDimension;
            
            // Other two are based on x and y dimensions.
            int blockCenterX = sourceWidth / 2 + cameraToCentroidDistance[START_X_INX];
            boundingBox[START_X_INX] = clamp( 0, sourceWidth - cubicOutputDimension, blockCenterX - (cubicOutputDimension / 2) );
            boundingBox[END_X_INX] = boundingBox[START_X_INX] + cubicOutputDimension;

            // Now, we must invert these numbers, WRT the width.
            int tempStart = sourceWidth - boundingBox[END_X_INX];
            int tempEnd = sourceWidth - boundingBox[START_X_INX];
            boundingBox[START_X_INX] = tempStart;
            boundingBox[END_X_INX] = tempEnd;
            
            int blockCenterY = sourceHeight / 2 + cameraToCentroidDistance[START_Y_INX];
            boundingBox[START_Y_INX] = clamp( 0, sourceHeight - cubicOutputDimension, blockCenterY - (cubicOutputDimension / 2) );
            boundingBox[END_Y_INX] = boundingBox[START_Y_INX] + cubicOutputDimension;            

            // Now, we must again invert these numbers, WRT the height.
            tempStart = sourceHeight - boundingBox[END_Y_INX];
            tempEnd = sourceHeight - boundingBox[START_Y_INX];
            boundingBox[START_Y_INX] = tempStart;
            boundingBox[END_Y_INX] = tempEnd;
            
            int szMod = (cubicOutputDimension) % BOUNDARY_MULTIPLE;
            // Force z dimension to a given multiple.
            if ( szMod != 0 ) {
                sz = (((cubicOutputDimension) / BOUNDARY_MULTIPLE) + 1 ) * BOUNDARY_MULTIPLE;
            }
            else {
                sz = cubicOutputDimension;
            }
            
            // Adjust the x and y dimensions, to adjust the subsetting requirement.
            sx = boundingBox[ END_X_INX ] - boundingBox[ START_X_INX ];
            sy = boundingBox[ END_Y_INX ] - boundingBox[ START_Y_INX ];
        }
        else {
            sz = zCount;
        }
        
        int sheetSize = sx * sy;
        final int totalVoxels = sheetSize * sz;
        pixelBytes = (int)Math.floor( file.length() / ((sourceWidth*sourceHeight) * sheetCountFromFile) );
        //System.out.println("File size is " + file.length());
        //System.out.println("Expected number of voxels = " + (sourceWidth*sourceHeight) * sheetCountFromFile );
        if ( pixelBytes < 4  ||  cubicOutputDimension != -1 ) {
            textureByteArray = new byte[totalVoxels * pixelBytes];
        }
        else {
            argbTextureIntArray = new int[totalVoxels];
        }

        // DEBUG CODE>
        for ( int i = 0; i < 3; i++ ) {
            System.out.println( "Bounding Box offset=" + i + " value=" + boundingBox[i] );
            System.out.println( "Bounding Box offset=" + (i+3) + " value=" + boundingBox[i+3] );
            System.out.println();
        }
        return sheetSize;
    }
    
    private int clamp( int min, int max, int startingValue ) {
        if ( startingValue < min ) {
            return min;
        }
        else if ( startingValue > max ) {
            return max;
        }
        return startingValue;
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
    
    private interface DataMover {
        void moveData(int sourceOffset, int destOffset);
    }
    
    private class ByteDataMover implements DataMover {
        private final byte[] pixels;
        
        public ByteDataMover(byte[] pixels) {
            this.pixels = pixels;
        }
        
        @Override
        public void moveData( int sourceOffset, int destOffset ) {
            System.arraycopy(pixels, sourceOffset, textureByteArray, destOffset, pixelBytes);            
        }
    }
    
    private class ShortDataMover implements DataMover {
        private final short[] pixels;
        
        public ShortDataMover(short[] pixels) {
            this.pixels = pixels;
        }
        
        @Override
        public void moveData( int sourceOffset, int destOffset ) {            
            // Changing the order.
            int unsignedPixelVal = pixels[ sourceOffset ];
            if (unsignedPixelVal < 0) {
                unsignedPixelVal += 65536;
            }
            byte byteVal = (byte) ((unsignedPixelVal & 0x0000ff00) >> 8);
            textureByteArray[ destOffset + 1 ] = byteVal;
            byteVal = (byte) (unsignedPixelVal & 0x000000ff);
            textureByteArray[ destOffset ] = byteVal;
        }
    }
    
    private class IntDataMover implements DataMover {
        private final int[] pixels;
        
        public IntDataMover(int[] pixels) {
            this.pixels = pixels;
        }
        
        @Override
        public void moveData( int sourceOffset, int destOffset ) {
            // Changing the order.
            long value = pixels[sourceOffset];
            if ( value < 0 ) {
                value += Integer.MAX_VALUE;
            }
            for (int pi = 0; pi < pixelBytes; pi++) {
                byte piByte = (byte) (value >>> (pi * 8) & 0x000000ff);
                textureByteArray[ destOffset + pi ] = piByte;
            }
        }
    }
    
}
