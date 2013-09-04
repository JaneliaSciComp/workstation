package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_export;

import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.AbstractAcceptorDecorator;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.ChannelMetaData;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanDataAcceptorI;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.RenderMappingI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 7/26/13
 * Time: 3:22 PM
 *
 * Apply this decorator if recoloring of masked regions is required on the CPU side, not the GPU side.
 */
public class RecoloringAcceptorDecorator  extends AbstractAcceptorDecorator {
    private Logger logger = LoggerFactory.getLogger(RecoloringAcceptorDecorator.class);
    private Map<ChannelMetaData,ChannelMetaData> metaDataMap;

    public RecoloringAcceptorDecorator( MaskChanDataAcceptorI acceptor ) {
        setWrappedAcceptor( acceptor );
    }

    @Override
    public int addChannelData(byte[] channelData, long position, long x, long y, long z, ChannelMetaData channelMetaData) throws Exception {
        int returnVal = 0;
        if ( channelMetaData.renderableBean != null   &&   channelMetaData.renderableBean.getRgb()[ 3 ] != RenderMappingI.PASS_THROUGH_RENDERING ) {
            if ( channelMetaData.channelCount == 3  ||  channelMetaData.channelCount == 4 ) {
                int maxIntensity = getMaxIntensity( channelData, channelMetaData );
                // Iterate over all the channels of information.
                int[] rgbIndexes = channelMetaData.getOrderedRgbIndexes();
                for ( int i = 0; i < 3; i++ ) {
                    // NOTE: expect channel count to be exactly 3.  The 4th channel is unused, here.
                    substituteChannelData(channelData, channelMetaData, maxIntensity, rgbIndexes[i], i);
                }

                returnVal = wrappedAcceptor.addChannelData( channelData, position, x, y, z, channelMetaData );
            }
            else if ( channelMetaData.channelCount == 1 ) {
                ChannelMetaData substitutedChannelMetaData = getLazySubsituteChannelData(channelMetaData);

                // In this case, would like to expand the number of channels and fill them with the user-given values.
                int maxIntensity = getMaxIntensity( channelData, channelMetaData );
                byte[] assumedChannelData = new byte[ channelMetaData.byteCount * 3 ];
                for ( int i = 0; i < 3; i++ ) {
                    substituteChannelData(assumedChannelData, substitutedChannelMetaData, maxIntensity, i, i);
                }
                returnVal = wrappedAcceptor.addChannelData( assumedChannelData, position, x, y, z, substitutedChannelMetaData );
            }
            else {
                logger.error( "Unexpected channel count  of {}.  Expected only 3,4, or 1", channelMetaData.channelCount );
                throw new RuntimeException( "Internal error.  Please refer to log output." );
            }
        }
        return returnVal;
    }

    @Override
    public int addMaskData(Integer maskNumber, long position, long x, long y, long z) throws Exception {
        return wrappedAcceptor.addMaskData( maskNumber, position, x, y, z );
    }

    /** Finds maximum "color" or "channel" value among all channel data. */
    private int getMaxIntensity(byte[] channelData, ChannelMetaData channelMetaData) {
        int maxIntensity = 0;
        for ( int i = 0; i < channelMetaData.channelCount; i++ ) {
            // First find the maximum intensity values
            int channelIntensity = 0;
            for ( int j = 0; j < channelMetaData.byteCount; j++ ) {
                // Assumes big-endian.
                channelIntensity += channelData[ i * channelMetaData.byteCount + j ] << ( 8 * ( channelMetaData.byteCount - j - 1 ) );
            }
            if ( channelIntensity > maxIntensity )
                maxIntensity = channelIntensity;

        }
        return maxIntensity;
    }

    /** Channel data for some objects (at t-o-w, compartments) includes only a single byte, but this lets us use more powerful coloring systems. */
    private ChannelMetaData getLazySubsituteChannelData(ChannelMetaData channelMetaData) {
        if ( metaDataMap == null ) {
            metaDataMap = new HashMap<ChannelMetaData,ChannelMetaData>();
        }
        ChannelMetaData substitutedChannelMetaData = metaDataMap.get( channelMetaData );
        if ( substitutedChannelMetaData == null ) {
            substitutedChannelMetaData = channelMetaData.clone();
            substitutedChannelMetaData.channelCount = 3;
            substitutedChannelMetaData.rawChannelCount = 3;
            metaDataMap.put( channelMetaData, substitutedChannelMetaData );
        }
        return substitutedChannelMetaData;
    }

    private void substituteChannelData(byte[] channelData, ChannelMetaData channelMetaData, int maxIntensity, int rgbIndex, int i) {
        byte channelAssignment = channelMetaData.renderableBean.getRgb()[ 2 - rgbIndex];
        int channelValue = (int)((channelAssignment < 0 ? (256 + channelAssignment) : channelAssignment) * (maxIntensity/256.0f));
        for ( int j = 0; j < channelMetaData.byteCount; j++ ) {
            int nextPart = (channelValue >> ( 8 * (channelMetaData.byteCount - j - 1) ) ) & 0xff;
            channelData[ i * channelMetaData.byteCount + j ] = (byte)nextPart;
        }
    }

}
