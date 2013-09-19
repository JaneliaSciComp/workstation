package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_export;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.TIFFEncodeParam;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.VolumeDataI;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.jai.RasterFactory;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 4/16/13
 * Time: 4:18 PM
 *
 * Allows caller to export image data suitable for GPU upload (raw volume as 1D byte array), as a TIF file.
 */
public class TiffExporter {
    private static final int MAX_WRITEBACK_THREADS = 5;
    private final Logger logger = LoggerFactory.getLogger( TiffExporter.class );
    private int[] texIntArray;
    private short[] texShortArray;

    private enum VoxelType {
        BYTE, SHORT, INT
    }

    /**
     * Construct with temp-file name. Uses TIFF format, and tif/tiff extensions.
     */
    public TiffExporter() {
        super();
    }

    /** Exports a tiff stack representing all planes of the input texture. */
    public void export( TextureDataI texture, File chosenFile ) throws Exception {

        if ( chosenFile != null ) {
            chosenFile = enforcePreferredExtension(chosenFile);

            //analyzeByteBuffer( texture.getTextureData() );
            int textureSize = texture.getSz() * texture.getSy() * texture.getSx();
            VoxelType voxelType = getVoxelType( texture );
            if ( voxelType == VoxelType.INT ) {
                initTexIntArray(texture, textureSize);
            }
            else if ( voxelType == VoxelType.SHORT ) {
                initTexShortArray(texture, textureSize);
            }

            logger.info( "Exporting texture {}.  Size={}", texture.getFilename(), textureSize );

            Collection<BufferedImage> imageList = new ArrayList<BufferedImage>( texture.getSz() );
            ExecutorService compartmentsThreadPool = Executors.newFixedThreadPool( MAX_WRITEBACK_THREADS );
            for ( int z = 0; z < texture.getSz(); z++ ) {
                SliceLoadWorker sliceLoadWorker = new SliceLoadWorker( texture, z, textureSize, imageList );
                compartmentsThreadPool.execute( sliceLoadWorker );

            }

            logger.info("Awaiting shutdown.");
            compartmentsThreadPool.shutdown();
            compartmentsThreadPool.awaitTermination( 10, TimeUnit.MINUTES );
            logger.info("Thread pool termination complete.");

            OutputStream os = new BufferedOutputStream( new FileOutputStream( chosenFile ) );
            TIFFEncodeParam params = new TIFFEncodeParam();
            params.setLittleEndian( true );

            ImageEncoder ienc = ImageCodec.createImageEncoder( "tiff", os, params );
            BufferedImage nextImage = imageList.iterator().next();
            params.setExtraImages( imageList.iterator() );
            ienc.encode( nextImage );

            os.close();
        }
    }

    /** Exports a single tiff based on the input image. */
    public void export( BufferedImage image, File chosenFile ) throws Exception {
        if ( chosenFile != null ) {
            chosenFile = enforcePreferredExtension(chosenFile);

            logger.info( "Exporting image of area {}.", image.getHeight() * image.getWidth() );

            OutputStream os = new BufferedOutputStream( new FileOutputStream( chosenFile ) );
            TIFFEncodeParam params = new TIFFEncodeParam();
            params.setLittleEndian( true );

            ImageEncoder ienc = ImageCodec.createImageEncoder( "tiff", os, params );
            ienc.encode( image );

            os.close();
        }
    }

    private File enforcePreferredExtension(File chosenFile) {
        if (! chosenFile.getName().contains( "." ) ) {
            chosenFile = new File( chosenFile.getAbsolutePath() + ".tiff" );
        }
        return chosenFile;
    }

    private short[] getShortArray(int textureSize, ByteBuffer byteBuffer) {
        ShortBuffer argbBuffer = byteBuffer.asShortBuffer();
        argbBuffer.rewind();
        short[] argb;
        if ( argbBuffer.hasArray() )  {
            argb = argbBuffer.array();
        }
        else {
            argb = new short[ textureSize ];
            logger.debug( "Size of argb buffer is {}.", argbBuffer.remaining() );
            argbBuffer.get( argb );
        }
        return argb;
    }

    private int[] getIntArray(int textureSize, ByteBuffer byteBuffer) {
        IntBuffer argbBuffer = byteBuffer.asIntBuffer();
        argbBuffer.rewind();
        int[] argb;
        if ( argbBuffer.hasArray() )  {
            argb = argbBuffer.array();
        }
        else {
            argb = new int[ textureSize ];
            logger.debug( "Size of int buffer is {}.", argbBuffer.remaining() );
            argbBuffer.get( argb );
        }
        return argb;
    }

    public void close() {

    }

    private BufferedImage createBufferedImage(
            TextureDataI textureData, int sliceNum, int textureSize, VoxelType type
    ) {
        BufferedImage rtnVal = null;
        try {
            int bufImgType = BufferedImage.TYPE_BYTE_GRAY;
            if ( type == VoxelType.SHORT )
                bufImgType = BufferedImage.TYPE_USHORT_GRAY;
            else if ( type == VoxelType.INT )
                bufImgType = BufferedImage.TYPE_4BYTE_ABGR;

            if ( type == VoxelType.INT ) {
                rtnVal = getFlatBufferedImage(textureData, sliceNum, texIntArray, bufImgType);
            }
            else {
                rtnVal = getBufferedImage(textureData, sliceNum, textureSize, type, bufImgType);
            }

        } catch (Exception e) {
            logger.error( e.getMessage() );
            e.printStackTrace();
        }

        return rtnVal;
    }

    private BufferedImage getFlatBufferedImage(TextureDataI textureData, int sliceNum, int[] texIntarray, int bufImgType) {
        BufferedImage rtnVal;
        int sliceSize = textureData.getSx() * textureData.getSy();
        int sliceOffset = sliceNum * sliceSize;
        rtnVal = new BufferedImage( textureData.getSx(), textureData.getSy(), bufImgType );

        rtnVal.setRGB( 0, 0, textureData.getSx(), textureData.getSy(), texIntarray, sliceOffset, textureData.getSx() );
        return rtnVal;
    }

    private BufferedImage getBufferedImage(TextureDataI textureData, int sliceNum, int textureSize, VoxelType type, int bufImgType) {
        BufferedImage rtnVal;
        int sliceSize = textureData.getSx() * textureData.getSy();
        int sliceOffset = sliceNum * sliceSize;
        rtnVal = new BufferedImage( textureData.getSx(), textureData.getSy(), bufImgType );

        DataBuffer dataBuffer = createDataBuffer( textureData, textureSize, sliceSize, sliceOffset, type );

        int dataTypeSize = DataBuffer.getDataTypeSize( dataBuffer.getDataType() );
        Raster raster = RasterFactory.createPackedRaster(
                dataBuffer,
                textureData.getSx(),
                textureData.getSy(),
                dataTypeSize,
                new Point(0, 0)
        );
        rtnVal.setData(raster);
        return rtnVal;
    }

    private DataBuffer createDataBuffer(
            TextureDataI textureData, int textureSize, int sliceSize, int sliceOffset, VoxelType type
    ) {
        logger.info("Creating data buffer for type {}.", type );
        DataBuffer rtnVal = null;
        switch ( type ) {
            case BYTE :
            {
                VolumeDataI data = textureData.getTextureData();
                rtnVal = new DataBufferByte( data.getCurrentVolumeData(), sliceSize, sliceOffset );
                break;
            }
            case INT:
            {
                rtnVal = new DataBufferInt(texIntArray, sliceSize, sliceOffset );
                break;
            }
            case SHORT:
            {
                rtnVal = new DataBufferUShort( texShortArray, sliceSize, sliceOffset );
                break;
            }
        }
        return rtnVal;

    }

    /** Encode the texture's target upload type as an enum constant. */
    private VoxelType getVoxelType( TextureDataI texture ) {
        if ( texture.getPixelByteCount() == 2 ) {
            return VoxelType.SHORT;
        }
        else if ( texture.getChannelCount() >= 3  &&  texture.getPixelByteCount() == 1 ) {
            return VoxelType.INT;
        }
        else {
            return VoxelType.BYTE;
        }
    }

    // DEBUG CODE: find out if anything useful is in this array.
    private void analyzeIntBuff( int[] intArr, int sliceNum ) {
        logger.info("Checking for non-zeros in slice {}.", sliceNum);
        int nonZeroCount = 0;
        int[] positionCount = new int[ 4 ];
        for ( int i = 0; i < intArr.length; i++ ) {
            if ( intArr[ i ] != 0 ) {
                nonZeroCount ++;
                // Try and determine whether the alpha byte is set properly.
                //  NOTE: hi-byte yields nada.
                int hiByte = (intArr[ i ] >>> 24) & 0xff;
                if ( hiByte != 0 ) {
                    positionCount[ 0 ] ++;
                }
                else {
                    intArr[i] = intArr[ i ] | (0xff << 24);
                }

                int loByte = (intArr[ i ] & 0xff);
                if ( loByte != 0 ) {
                    positionCount[ 3 ] ++;
                }

                int byteVal = (intArr[ i ] >>> 16 ) & 0xff;
                if ( byteVal != 0 ) {
                    positionCount[ 1 ] ++;
                }

                byteVal = (intArr[i] >>> 8 ) & 0xff;
                if ( byteVal != 0 ) {
                    positionCount[ 2 ] ++;
                }
            }
        }

        if ( nonZeroCount > 0 ) {
            for ( int i = 0; i < 4; i++ ) {
                if ( positionCount[ i ] != 0 ) {
                    logger.info( "Position {} has {} non-zero values.", i, positionCount[ i ] );
                }
            }
        }

    }

    private int[] initTexIntArray(TextureDataI textureData, int textureSize) {
        if ( texIntArray == null ) {
            ByteBuffer byteBuffer = ByteBuffer.wrap( textureData.getTextureData().getCurrentVolumeData() );
            byteBuffer.rewind();
            byteBuffer.order( ByteOrder.LITTLE_ENDIAN );
            texIntArray = getIntArray( textureSize, byteBuffer );
        }
        return texIntArray;
    }

    private short[] initTexShortArray(TextureDataI textureData, int textureSize) {
        if ( texShortArray == null ) {
            ByteBuffer byteBuffer = ByteBuffer.wrap( textureData.getTextureData().getCurrentVolumeData() );
            byteBuffer.rewind();
            byteBuffer.order( ByteOrder.LITTLE_ENDIAN );
            texShortArray = getShortArray( textureSize, byteBuffer );
        }
        return texShortArray;
    }

    private void analyzeByteBuffer( byte[] resultingArray ) {
        int[] positionCount = new int[ 4 ];
        for ( int i = 0; i < resultingArray.length; i += 4 ) {
            for ( int pos = 0; pos < 4; pos++ ) {
                if ( resultingArray[ i + pos ] != 0 ) {
                    positionCount[ pos ]++;
                }
            }
        }
        for ( int i = 0; i < positionCount.length; i++ ) {
            logger.info( "Position {} has {} non-zero bytes.", i, positionCount[ i ] );
        }
    }

    class SliceLoadWorker extends SimpleWorker {

        private final TextureDataI texture;
        private final int z;
        private final int textureSize;
        private final Collection<BufferedImage> imageList;

        public SliceLoadWorker(
                TextureDataI texture, int z, int textureSize, Collection<BufferedImage> imageList
        ) {
            this.texture = texture;
            this.z = z;
            this.textureSize = textureSize;
            this.imageList = imageList;
        }

        @Override
        protected void doStuff() throws Exception {
            BufferedImage slice;
            VoxelType voxelType = getVoxelType( texture );
            slice = createBufferedImage( texture, z, textureSize, voxelType );
            imageList.add( slice );
        }

        @Override
        protected void hadSuccess() {
        }

        @Override
        protected void hadError(Throwable error) {
            SessionMgr.getSessionMgr().handleException( error );
        }
    }

}

