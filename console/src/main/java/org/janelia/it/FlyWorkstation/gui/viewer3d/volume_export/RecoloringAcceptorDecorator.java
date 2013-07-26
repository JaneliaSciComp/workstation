package org.janelia.it.FlyWorkstation.gui.viewer3d.volume_export;

import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.ChannelMetaData;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanDataAcceptorI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.RenderMappingI;
import org.slf4j.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 7/26/13
 * Time: 3:22 PM
 *
 * Apply this decorator if recoloring of masked regions is required on the CPU side, not the GPU side.
 */
public class RecoloringAcceptorDecorator  implements MaskChanDataAcceptorI {
    private MaskChanDataAcceptorI wrappedAcceptor;
    public RecoloringAcceptorDecorator( MaskChanDataAcceptorI acceptor ) {
        this.wrappedAcceptor = acceptor;
    }

    @Override
    public int addChannelData(byte[] channelData, long position, long x, long y, long z, ChannelMetaData channelMetaData) throws Exception {
        if ( channelMetaData.renderableBean != null   &&   channelMetaData.renderableBean.getRgb()[ 3 ] != RenderMappingI.PASS_THROUGH_RENDERING ) {
            if ( channelMetaData.channelCount == 3  ||  channelMetaData.channelCount == 4 ) {
                // Iterate over all the channels of information.
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
                for ( int i = 0; i < 3; i++ ) {
                    // NOTE: expect channel count to be exactly 3.
                    int[] rgbIndexes = channelMetaData.getOrderedRgbIndexes();
                    byte channelAssignment = channelMetaData.renderableBean.getRgb()[ 2 - rgbIndexes[ i ] ];
                    int channelValue = (channelAssignment < 0 ? (256 + channelAssignment) : channelAssignment) * maxIntensity;
                    for ( int j = 0; j < channelMetaData.byteCount; j++ ) {
                        int nextPart = (channelValue >> ( 8 * (channelMetaData.byteCount - j - 1) ) ) & 0xff;
                        channelData[ i * channelMetaData.byteCount + j ] = (byte)nextPart;
                    }
                }

            }
//            else if ( channelMetaData.channelCount == 1 ) {
//                int dummyVal = 0;
//            }
        }
        return wrappedAcceptor.addChannelData( channelData, position, x, y, z, channelMetaData );
    }

    @Override
    public int addMaskData(Integer maskNumber, long position, long x, long y, long z) throws Exception {
        return wrappedAcceptor.addMaskData( maskNumber, position, x, y, z );
    }

    @Override
    public void setSpaceSize(long x, long y, long z, long paddedX, long paddedY, long paddedZ, float[] coordCoverage) {
        wrappedAcceptor.setSpaceSize( x, y, z, paddedX, paddedY, paddedZ, coordCoverage );
    }

    @Override
    public Acceptable getAcceptableInputs() {
        return wrappedAcceptor.getAcceptableInputs();
    }

    @Override
    public int getChannelCount() {
        return wrappedAcceptor.getChannelCount();
    }

    @Override
    public void setChannelMetaData(ChannelMetaData metaData) {
        wrappedAcceptor.setChannelMetaData( metaData );
    }

    @Override
    public void endData(Logger logger) {
        wrappedAcceptor.endData( logger );
    }

}
