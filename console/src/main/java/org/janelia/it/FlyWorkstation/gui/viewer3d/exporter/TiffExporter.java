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

//    private File tifFile;

    /**
     * Construct with temp-file name. Uses TIFF format, and tif/tiff extensions.
     */
    public TiffExporter() throws IOException {
        super();
    }

    public void export( TextureDataI texture, File chosenFile ) throws Exception {

        chosenFile = enforcePreferredExtension(chosenFile);

        if ( chosenFile != null ) {
            int textureSize = texture.getSz() * texture.getSy() * texture.getSx();
            logger.info( "Exporting texture {}.  Size={}", texture.getFilename(), textureSize );
            ByteBuffer byteBuffer = ByteBuffer.wrap( texture.getTextureData() );
            byteBuffer.rewind();
            byteBuffer.order( ByteOrder.LITTLE_ENDIAN );

            short[] argb = null;
            if ( texture.getPixelByteCount() == 2 ) {
                argb = getShortArray(textureSize, byteBuffer);
            }

            Collection<BufferedImage> imageList = new ArrayList<BufferedImage>( texture.getSz() );
            for ( int z = 0; z < texture.getSz(); z++ ) {
                BufferedImage slice = null;
                if ( texture.getPixelByteCount() == 2 ) {
                    slice = createBufferedImage( texture, argb, z );
                }
                else {
                    slice = createBufferedImage( texture, texture.getTextureData(), z );
                }
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

    public void close() {

    }

    private BufferedImage createBufferedImage( TextureDataI textureData, short[] argb, int sliceNum ) {
        BufferedImage rtnVal = null;
        try {

            int sliceSize = textureData.getSx() * textureData.getSy();
            int sliceOffset = sliceNum * sliceSize;
            rtnVal = new BufferedImage( textureData.getSx(), textureData.getSy(), BufferedImage.TYPE_USHORT_GRAY );
            DataBuffer dataBuffer = new DataBufferUShort( argb, sliceSize, sliceOffset );
            Raster raster = RasterFactory.createPackedRaster(
                    dataBuffer, textureData.getSx(), textureData.getSy(), 16, new Point(0, 0)
            );
            rtnVal.setData( raster );

        } catch (Exception e) {
            logger.error( e.getMessage() );
            e.printStackTrace();
        }

        return rtnVal;
    }

    private BufferedImage createBufferedImage( TextureDataI textureData, byte[] argb, int sliceNum ) {
        BufferedImage rtnVal = null;
        try {

            int sliceSize = textureData.getSx() * textureData.getSy();
            int sliceOffset = sliceNum * sliceSize;
            rtnVal = new BufferedImage( textureData.getSx(), textureData.getSy(), BufferedImage.TYPE_BYTE_GRAY );
            DataBuffer dataBuffer = new DataBufferByte( argb, sliceSize, sliceOffset );
            Raster raster = RasterFactory.createPackedRaster(
                    dataBuffer, textureData.getSx(), textureData.getSy(), 8, new Point(0, 0)
            );
            rtnVal.setData( raster );

        } catch (Exception e) {
            logger.error( e.getMessage() );
            e.printStackTrace();
        }

        return rtnVal;
    }

}

