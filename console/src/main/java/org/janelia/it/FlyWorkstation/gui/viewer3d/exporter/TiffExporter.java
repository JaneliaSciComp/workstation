package org.janelia.it.FlyWorkstation.gui.viewer3d.exporter;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.TIFFEncodeParam;
//import loci.formats.IFormatWriter;
//import loci.formats.ImageWriter;
//import loci.formats.MetadataTools;
//import loci.formats.meta.MetadataRetrieve;
//import loci.formats.meta.MetadataStore;
//import loci.formats.out.TiffWriter;
//import loci.formats.tiff.IFD;
//import ome.xml.model.AffineTransform;
//import ome.xml.model.enums.*;
//import ome.xml.model.primitives.*;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import javax.imageio.ImageIO;
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
import java.util.Iterator;

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

    public void export( TextureDataI texture ) throws Exception {

        int textureSize = texture.getSz() * texture.getSy() * texture.getSx();
        logger.info( "Exporting texture {}.  Size={}", texture.getFilename(), textureSize );
        ByteBuffer byteBuffer = ByteBuffer.wrap( texture.getTextureData() );
        byteBuffer.rewind();
        byteBuffer.order( ByteOrder.LITTLE_ENDIAN );
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

        Collection<BufferedImage> imageList = new ArrayList<BufferedImage>( texture.getSz() );
        for ( int z = 0; z < texture.getSz(); z++ ) {
            BufferedImage slice = createBufferedImage( texture, argb, z );
            imageList.add( slice );
        }

        JFileChooser fileChooser = new JFileChooser( "Choose Export File" );
        fileChooser.setToolTipText( "Pick an output location for the exported file." );
        fileChooser.showOpenDialog( null );
        OutputStream os = new BufferedOutputStream( new FileOutputStream( fileChooser.getSelectedFile() ) );
        TIFFEncodeParam params = new TIFFEncodeParam();
        params.setLittleEndian( true );

        ImageEncoder ienc = ImageCodec.createImageEncoder( "tiff", os, params );
        BufferedImage nextImage = imageList.iterator().next();
        params.setExtraImages( imageList.iterator() );
        ienc.encode( nextImage );

        os.close();
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
}
