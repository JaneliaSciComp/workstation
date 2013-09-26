package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_export;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.TIFFEncodeParam;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.VolumeDataI;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.volume_builder.VolumeDataChunk;
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
    public static final int BYTES_PER_INT = (Integer.SIZE / 8);
    public static final int BYTES_PER_SHORT = (Short.SIZE / 8);
    private final Logger logger = LoggerFactory.getLogger( TiffExporter.class );
    private int[][] texIntArray;
    private short[][] texShortArray;

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
            int sliceSize = texture.getSy() * texture.getSx();
            int textureSize = texture.getSz() * sliceSize;
            VoxelType voxelType = getVoxelType( texture );
            if ( voxelType == VoxelType.INT ) {
                initTexIntArray(texture);
            }
            else if ( voxelType == VoxelType.SHORT ) {
                initTexShortArray(texture);
            }

            logger.info( "Exporting texture {}.  Size={}", texture.getFilename(), textureSize );

            Collection<BufferedImage> imageList = new ArrayList<BufferedImage>( texture.getSz() );
            ExecutorService threadPool = Executors.newFixedThreadPool( MAX_WRITEBACK_THREADS );
            VolumeDataChunk[] volumeChunks = texture.getTextureData().getVolumeChunks();
            int absoluteSliceNum = 0;
            for ( int chunkNum = 0; chunkNum < volumeChunks.length; chunkNum ++ ) {
                byte[] data = volumeChunks[chunkNum].getData();
                int slicesPerChunk = volumeChunks[chunkNum].getDepth();
                for ( int z = 0; z < slicesPerChunk; z++ ) {

                    SliceLoadWorkerParam param = new SliceLoadWorkerParam();
                    param.setChunkNum( chunkNum );
                    param.setImageList( imageList );
                    param.setSize( data.length );
                    param.setVoxelType( voxelType );
                    param.setData( data );
                    param.setRelativeZ( z );
                    param.setAbsoluteZ( absoluteSliceNum );
                    param.setOffset( sliceSize * z );

                    //texture, z, textureSize, imageList
                    SliceLoadWorker sliceLoadWorker = new SliceLoadWorker( param, texture );
                    threadPool.execute(sliceLoadWorker);

                    absoluteSliceNum ++;
                }
            }

            logger.info("Awaiting shutdown.");
            threadPool.shutdown();
            threadPool.awaitTermination(10, TimeUnit.MINUTES);
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
            TextureDataI textureData, int chunkNum, int sliceNum, VoxelType type
    ) {
        BufferedImage rtnVal = null;
        try {
            int bufImgType = BufferedImage.TYPE_BYTE_GRAY;
            if ( type == VoxelType.SHORT )
                bufImgType = BufferedImage.TYPE_USHORT_GRAY;
            else if ( type == VoxelType.INT )
                bufImgType = BufferedImage.TYPE_4BYTE_ABGR;

            if ( type == VoxelType.INT ) {
                rtnVal = getFlatBufferedImage(textureData, sliceNum, texIntArray[chunkNum], bufImgType);
            }
            else {
                rtnVal = getBufferedImage(textureData, chunkNum, sliceNum, type, bufImgType);
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

    private BufferedImage getBufferedImage(TextureDataI textureData, int chunkNum, int sliceNum, VoxelType type, int bufImgType) {
        BufferedImage rtnVal;
        int sliceSize = textureData.getSx() * textureData.getSy();
        int sliceOffset = sliceNum * sliceSize;
        rtnVal = new BufferedImage( textureData.getSx(), textureData.getSy(), bufImgType );

        DataBuffer dataBuffer = createDataBuffer( textureData, chunkNum, sliceSize, sliceOffset, type );

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
            TextureDataI textureData, int chunkNum, int sliceSize, int sliceOffset, VoxelType type
    ) {
        logger.info("Creating data buffer for type {}.", type );
        DataBuffer rtnVal = null;
        switch ( type ) {
            case BYTE :
            {
                VolumeDataI data = textureData.getTextureData();

                rtnVal = new DataBufferByte( data.getVolumeChunks()[ chunkNum ].getData(), sliceSize, sliceOffset );
                break;
            }
            case INT:
            {
                rtnVal = new DataBufferInt( texIntArray[ chunkNum ], sliceSize, sliceOffset );
                break;
            }
            case SHORT:
            {
                rtnVal = new DataBufferUShort( texShortArray[ chunkNum ], sliceSize, sliceOffset );
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

    // DEBUG CODE: find out if anything useful is in this array.  Call these as needed.
    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
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

    private int[][] initTexIntArray(TextureDataI textureData) {
        if ( texIntArray == null ) {
            VolumeDataChunk[] volumeChunks = textureData.getTextureData().getVolumeChunks();
            int numChunks = volumeChunks.length;
            texIntArray = new int[ numChunks ][];
            for ( int i = 0; i < numChunks; i++ ) {
                ByteBuffer byteBuffer = ByteBuffer.wrap(volumeChunks[i].getData());
                byteBuffer.rewind();
                byteBuffer.order( ByteOrder.LITTLE_ENDIAN );
                texIntArray[ i ] = getIntArray(
                        volumeChunks[i].getData().length / BYTES_PER_INT, byteBuffer
                );
            }
        }
        return texIntArray;
    }

    private short[][] initTexShortArray(TextureDataI textureData) {
        if ( texShortArray == null ) {
            VolumeDataChunk[] volumeChunks = textureData.getTextureData().getVolumeChunks();
            int numChunks = volumeChunks.length;
            texShortArray = new short[ numChunks ][];
            for ( int i = 0; i < numChunks; i++ ) {
                ByteBuffer byteBuffer = ByteBuffer.wrap(volumeChunks[i].getData());
                byteBuffer.rewind();
                byteBuffer.order( ByteOrder.LITTLE_ENDIAN );
                texShortArray[ i ] = getShortArray(
                        volumeChunks[i].getData().length / BYTES_PER_SHORT, byteBuffer
                );
            }
        }
        return texShortArray;
    }

    class SliceLoadWorker extends SimpleWorker {

        private TextureDataI texture;
        private SliceLoadWorkerParam param;

        public SliceLoadWorker(
                SliceLoadWorkerParam param,
                TextureDataI texture
        ) {
            this.texture = texture;
            this.param = param;
        }

        @Override
        protected void doStuff() throws Exception {
            BufferedImage slice;
            slice = createBufferedImage( texture, param.getChunkNum(), param.getRelativeZ(), param.getVoxelType() );
            param.getImageList().add(slice);
        }

        @Override
        protected void hadSuccess() {
        }

        @Override
        protected void hadError(Throwable error) {
            SessionMgr.getSessionMgr().handleException( error );
        }

    }

    class SliceLoadWorkerParam {
        private byte[] data;
        private int size;
        private int offset;
        private int chunkNum;
        private Collection<BufferedImage> imageList;
        private int absoluteZ;
        private int relativeZ;
        private VoxelType voxelType;

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data) {
            this.data = data;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public int getOffset() {
            return offset;
        }

        public void setOffset(int offset) {
            this.offset = offset;
        }

        public Collection<BufferedImage> getImageList() {
            return imageList;
        }

        public void setImageList(Collection<BufferedImage> imageList) {
            this.imageList = imageList;
        }

        public int getAbsoluteZ() {
            return absoluteZ;
        }

        public void setAbsoluteZ(int z) {
            this.absoluteZ = z;
        }

        public int getRelativeZ() {
            return relativeZ;
        }

        public void setRelativeZ(int z) {
            this.relativeZ = z;
        }

        public VoxelType getVoxelType() {
            return voxelType;
        }

        public void setVoxelType(VoxelType voxelType) {
            this.voxelType = voxelType;
        }

        public int getChunkNum() {
            return chunkNum;
        }

        public void setChunkNum(int chunkNum) {
            this.chunkNum = chunkNum;
        }
    }

}

