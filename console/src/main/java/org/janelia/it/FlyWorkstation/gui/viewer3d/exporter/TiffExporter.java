package org.janelia.it.FlyWorkstation.gui.viewer3d.exporter;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.TIFFEncodeParam;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.jai.RasterFactory;
import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 4/16/13
 * Time: 4:18 PM
 *
 * Allows caller to export image data suitable for GPU upload (raw volume as 1D byte array), as a TIF file.
 */
public class TiffExporter {
    private Logger logger = LoggerFactory.getLogger( TiffExporter.class );

    private enum VoxelType {
        BYTE, SHORT, INT
    }

    /**
     * Construct with temp-file name. Uses TIFF format, and tif/tiff extensions.
     */
    public TiffExporter() throws IOException {
        super();
    }

    /** Exports a tiff stack representing all planes of the input texture. */
    public void export( TextureDataI texture, File chosenFile ) throws Exception {

        if ( chosenFile != null ) {
            chosenFile = enforcePreferredExtension(chosenFile);

            int textureSize = texture.getSz() * texture.getSy() * texture.getSx();
            logger.info( "Exporting texture {}.  Size={}", texture.getFilename(), textureSize );

            VoxelType voxelType = getVoxelType( texture );

            Collection<BufferedImage> imageList = new ArrayList<BufferedImage>( texture.getSz() );
            for ( int z = 0; z < texture.getSz(); z++ ) {
                BufferedImage slice;
                slice = createBufferedImage( texture, z, textureSize, voxelType );
                imageList.add( slice );
            }

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
            logger.info( "Size of argb buffer is {}.", argbBuffer.remaining() );
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
            logger.info( "Size of int buffer is {}.", argbBuffer.remaining() );
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
                int sliceSize = textureData.getSx() * textureData.getSy();
                int sliceOffset = sliceNum * sliceSize;
                rtnVal = new BufferedImage( textureData.getSx(), textureData.getSy(), bufImgType );

                ByteBuffer byteBuffer = ByteBuffer.wrap( textureData.getTextureData() );
                byteBuffer.rewind();
                byteBuffer.order( ByteOrder.LITTLE_ENDIAN );
                int[] intArr = getIntArray( textureSize, byteBuffer );
                rtnVal.setRGB( 0, 0, textureData.getSx(), textureData.getSy(), intArr, sliceOffset, textureData.getSx() );

            }
            else {
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
                        new Point( 0, 0 )
                );
                rtnVal.setData( raster );
            }

        } catch (Exception e) {
            logger.error( e.getMessage() );
            e.printStackTrace();
        }

        return rtnVal;
    }

    private DataBuffer createDataBuffer(
            TextureDataI textureData, int textureSize, int sliceSize, int sliceOffset, VoxelType type
    ) {
        DataBuffer rtnVal = null;
        switch ( type ) {
            case BYTE :
            {
                byte[] byteArr = textureData.getTextureData();
                rtnVal = new DataBufferByte( byteArr, sliceSize, sliceOffset );
                break;
            }
            case INT:
            {
                ByteBuffer byteBuffer = ByteBuffer.wrap( textureData.getTextureData() );
                byteBuffer.rewind();
                byteBuffer.order( ByteOrder.LITTLE_ENDIAN );

                int[] intArr = getIntArray( textureSize, byteBuffer );
                rtnVal = new DataBufferInt( intArr, sliceSize, sliceOffset );
                break;
            }
            case SHORT:
            {
                ByteBuffer byteBuffer = ByteBuffer.wrap( textureData.getTextureData() );
                byteBuffer.rewind();
                byteBuffer.order( ByteOrder.LITTLE_ENDIAN );

                short[] shortArr = getShortArray( textureSize, byteBuffer );
                rtnVal = new DataBufferUShort( shortArr, sliceSize, sliceOffset );
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

}

