package org.janelia.it.FlyWorkstation.gui.viewer3d.masking;

import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableBean;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/14/13
 * Time: 4:47 PM
 *
 * This test will check the efficacy of the renderables mask builder.
 */
public class RenderablesMaskBuilderTest {

    ///Users/fosterl/Documents/alignment_board/Mask_Chan
    private static final String TEST_FILE_NAME = "prefix_1.mask";
    private static final String LOCAL_FILE_PATH = "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_1.mask";

    private InputStream testStream;

    // The input data is known to be little-endian or LSB.
    private byte[] intArray = new byte[ 4 ];
    private byte[] longArray = new byte[ 8 ];

    private ByteBuffer intBuffer = ByteBuffer.wrap( intArray );
    {
        intBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    private ByteBuffer longBuffer = ByteBuffer.wrap( longArray );
    {
        longBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    @Before
    public void setup() throws  Exception {
        testStream = this.getClass().getResourceAsStream( TEST_FILE_NAME );
        if ( testStream == null ) {
            testStream = new FileInputStream( LOCAL_FILE_PATH );
        }
    }

    @After
    public void tearDown() throws Exception {
        testStream.close();
    }

/*
  Format for mask and channel files.
  Mask files:
  long xsize; // space
  long ysize; // space
  long zsize; // space
  long x0; // bounding box
  long x1; // bounding box, such that x0 is inclusive, x1 exclusive, etc
  long y0; // bb
  long y1; // bb
  long z0; // bb
  long z1; // bb
  long totalVoxels;
  unsigned char axis; // 0=yz(x), 1=xz(y), 2=xy(z)
  { // For each ray
    long skip;
    long pairs;
    { // For each pair
        long start;
        long end; // such that end-start is length, i.e., end is exclusive
    }
  }
  Channel files:
  long totalVoxels;
  unsigned char channels; // number of channels
  unsigned char recommendedRedChannel;
  unsigned char recommendedGreenChannel;
  unsigned char recommendedBlueChannel;
  unsigned char bytesPerChannel; // 1=8-bit, 2=16-bit
  { // For each channel
    { // For each voxel
        B value;
    }
  }
 */

    @Test
    public void readOneFile() throws Exception {
        Long sx = readLong( testStream );
        Long sy = readLong( testStream );
        Long sz = readLong( testStream );

        // Reading the bounding box starts/ends.
        Long[] boundsXCoords = new Long[2];
        boundsXCoords[ 0 ] = readLong( testStream );
        boundsXCoords[ 1 ] = readLong( testStream );

        Long[] boundsYCoords = new Long[2];
        boundsYCoords[ 0 ] = readLong( testStream );
        boundsYCoords[ 1 ] = readLong( testStream );

        Long[] boundsZCoords = new Long[2];
        boundsZCoords[ 0 ] = readLong( testStream );
        boundsZCoords[ 1 ] = readLong( testStream );

        Long totalVoxels = readLong( testStream );
        Byte axis = readByte( testStream );

        RenderablesMaskBuilder builder = new RenderablesMaskBuilder( sx, sy, sz );
        builder.setByteCount( 2 );
        builder.setChannelCount( 1 ); // Re-examine later: when using channel files.
        builder.setDimensionOrder( (int)axis );

        // Time-of-writing: only thing bean is used for is its tanslated number.
        RenderableBean bean = new RenderableBean();
        bean.setTranslatedNum( 1 );

        // public void addData( RenderableBean renderable, int skippedRayCount, int[][] pairsAlongRay ) throws Exception {
        long totalRead = 0;
        while ( totalRead < totalVoxels ) {
            Long skippedRayCount = readLong( testStream );
            Long pairCount = readLong( testStream );
            long[][] pairs = new long[ pairCount.intValue() ][ 2 ];
            for ( int i = 0; i < pairCount; i++ ) {
                pairs[ i ][ 0 ] = readLong( testStream );
                pairs[ i ][ 1 ] = readLong( testStream );
            }
            int nextRead = builder.addData( bean, skippedRayCount, pairs );
            totalRead += nextRead;
            if ( nextRead == 0 ) {
                throw new Exception("Zero bytes read.");
            }

        }
    }

    /**
     * Reads a single integer from the input stream, in LSB order.
     *
     * @param is an input stream pointing at data whose next value is an int.
     * @return next integer from the stream.
     * @throws Exception thrown by called methods, or if insufficient data remains.
     */
    private int readInt( InputStream is ) throws Exception {
        if ( is.read( intArray, 0, 4 ) < 4 ) {
            throw new Exception( "Unexpected end of file while reading an int." );
        }
        intBuffer.rewind();
        intBuffer.put( intArray );
        intBuffer.rewind();

        return intBuffer.getInt();
    }

    /**
     * Reads a single byte from the input stream, in LSB order.
     *
     * @param is an input stream pointing at data whose next value is a byte.
     * @return next byte from the stream.
     * @throws Exception thrown by called methods.
     */
    private byte readByte( InputStream is ) throws Exception {
        return (byte)is.read();
    }

    /**
     * Reads a single long from the input stream, in LSB order.
     *
     * @param is an input stream pointing at data whose next value is a long.
     * @return next long from the stream.
     * @throws Exception thrown by called methods, or if insufficient data remains.
     */
    private long readLong( InputStream is ) throws Exception {
        if ( is.read( longArray, 0, 8 ) < 8 ) {
            throw new Exception( "Unexpected end of file while reading a long." );
        }
        longBuffer.rewind();
        longBuffer.put( longArray );
        longBuffer.rewind();

        return longBuffer.getLong();
    }
}
