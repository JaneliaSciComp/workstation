package org.janelia.it.FlyWorkstation.gui.viewer3d.channel;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/15/13
 * Time: 11:19 AM
 *
 * This tests the read efficacy of channel files.
 */
public class ChannelReadTest {

    ///Users/fosterl/Documents/alignment_board/Mask_Chan
    private static final String TEST_FILE_NAME = "prefix_1.chan";
    private static final String LOCAL_FILE_PATH = "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_1.chan";

    private InputStream testStream;

    // The input data is known to be little-endian or LSB.
    private byte[] longArray = new byte[ 8 ];

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

    @Test
    public void readChannelData() throws Exception {
        Long totalVoxels = readLong( testStream );
        Byte numberOfChannels = readByte( testStream );
        Byte recommendedRedChannel = readByte( testStream );
        Byte recommendedGreenChannel = readByte( testStream );
        Byte recommendedBlueChannel = readByte( testStream );
        Byte bytesPerChannel = readByte( testStream );
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


}
