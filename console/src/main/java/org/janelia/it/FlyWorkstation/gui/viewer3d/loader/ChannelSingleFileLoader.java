package org.janelia.it.FlyWorkstation.gui.viewer3d.loader;

import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable.RenderableBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/13/13
 * Time: 4:21 PM
 *
 * Read in only channel data, producing a convenient return object.
 */
public class ChannelSingleFileLoader {

    private static final int LONG_BYTES = Long.SIZE / 8;
    public static final String EXCESSIVE_ARRAY_MSG = "Excessive array size encountered.  Scaling error.";

    // The input data is known to be little-endian or LSB.
    private byte[] longArray = new byte[ 8 ];
    private byte[] floatArray = new byte[ 4 ];
    private RenderableBean bean;

    private ByteBuffer longBuffer = ByteBuffer.wrap( longArray );
    {
        longBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    private ByteBuffer floatBuffer = ByteBuffer.wrap( floatArray );
    {
        floatBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    private Logger logger = LoggerFactory.getLogger( ChannelSingleFileLoader.class );

    public ChannelSingleFileLoader( RenderableBean bean ) {
        this.bean = bean;
    }

    /**
     * Fetch any channel-data required for this bean.  Also, the needs of acceptors will be taken into account;
     * there may be no need to read anything here at all.
     *
     * @return list of channel arrays, raw byte data.
     * @throws Exception thrown by any called method.
     */
    public ChannelDataBean readChannelData( InputStream channelStream, boolean readBytes ) throws Exception {
        ChannelDataBean returnValue = new ChannelDataBean();
        List<byte[]> channelByteArrayList = new ArrayList<byte[]>();

        // Open the file, and move pointers down to seek-ready point.
        Long totalIntensityVoxels = readLong(channelStream);

        ChannelMetaData channelMetaData = new ChannelMetaData();
        channelMetaData.rawChannelCount = readByte( channelStream );
        channelMetaData.channelCount = channelMetaData.rawChannelCount;
        channelMetaData.redChannelInx = readByte( channelStream );
        channelMetaData.greenChannelInx = readByte( channelStream );
        channelMetaData.blueChannelInx = readByte( channelStream );
        channelMetaData.byteCount = readByte( channelStream );
        channelMetaData.renderableBean = bean;

        if ( readBytes ) {
            // NOTE: if no channels needed, the intensity stream may be ignored.

            long channelTotalBytes = totalIntensityVoxels * channelMetaData.byteCount * channelMetaData.channelCount;
            if ( channelTotalBytes > Integer.MAX_VALUE ) {
                logger.error(EXCESSIVE_ARRAY_MSG);
                throw new Exception(EXCESSIVE_ARRAY_MSG);
            }

            // Pull in every channel's data.
            for ( int i = 0; i < channelMetaData.channelCount; i++ ) {
                byte[] nextChannelData = new byte[ totalIntensityVoxels.intValue() * channelMetaData.byteCount ];

                int bytesRead = channelStream.read(nextChannelData);
                if ( bytesRead  <  nextChannelData.length ) {
                    throw new Exception(
                            "Failed to read channel data for channel " + i + " read " + bytesRead + " bytes."
                    );
                }
                channelByteArrayList.add( nextChannelData );
            }
        }

        returnValue.setChannelData( channelByteArrayList );
        returnValue.setTotalVoxels( totalIntensityVoxels );
        returnValue.setChannelMetaData( channelMetaData );

        return returnValue;
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
        if ( is.read( longArray ) < LONG_BYTES) {
            throw new Exception( "Unexpected end of file while reading a long." );
        }
        // DEBUG
        //for ( int i = 0; i < LONG_BYTES; i++ ) {
        //    System.out.print( longArray[ i ] + " " );
        //}
        //System.out.println();
        longBuffer.rewind();

        return longBuffer.getLong();
    }

    /*
      Format for channel files.
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

    /**
     * Container bean for all relevant information to read from channel data.
     */
    public static class ChannelDataBean {
        private List<byte[]> channelData;
        private long totalVoxels;
        private ChannelMetaData channelMetaData;

        public List<byte[]> getChannelData() {
            return channelData;
        }

        public void setChannelData(List<byte[]> channelData) {
            this.channelData = channelData;
        }

        public long getTotalVoxels() {
            return totalVoxels;
        }

        public void setTotalVoxels(long totalVoxels) {
            this.totalVoxels = totalVoxels;
        }

        public ChannelMetaData getChannelMetaData() {
            return channelMetaData;
        }

        public void setChannelMetaData(ChannelMetaData channelMetaData) {
            this.channelMetaData = channelMetaData;
        }
    }

}
