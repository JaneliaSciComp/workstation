package org.janelia.it.FlyWorkstation.gui.viewer3d.masking;

import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/13/13
 * Time: 4:21 PM
 *
 * This implementation of a mask builder takes renderables as its driving data.  It will accept the renderables,
 * along with their applicable chunks of data, to produce its texture data volume, in memory.
 */
public class RenderablesChannelsBuilder extends RenderablesVolumeBuilder {

    //----------------------------------------IMPLEMENT MaskChanDataAcceptorI
    /**
     * This is called with data to be loaded.
     *
     * @param maskNumber describes all points belonging to all pairs.
     * @param position where in the linear volume coords does this go?
     * @return total positions applied.
     * @throws Exception thrown by called methods or if bad inputs are received.
     */
    @Override
    public int addMaskData(Integer maskNumber, long position) throws Exception {
        throw new IllegalArgumentException( "Not implemented" );
    }

    /**
     * Go to position indicated, and add this array of raw bytes.
     *
     * @param channelData what goes in the final volume.
     * @param position where it goes. Width of channel data is expected to be same for each call.
     * @return total positions applied.
     * @throws Exception
     */
    @Override
    public int addChannelData(byte[] channelData, long position) throws Exception {
        // Assumed little-endian and two bytes.
        byte[] data = super.getVolumeData();
        for ( int j = 0; j < channelData.length; j++ ) {
            data[ j + (int)position * channelData.length ] = channelData[ j ];
        }

        return 1;
    }

    /**
     * This tells the caller: only call me with channel data.  This is a channel-data builder.
     * @return channel
     */
    @Override
    public Acceptable getAcceptableInputs() {
        return Acceptable.channel;
    }

    //-------------------------END:-----------IMPLEMENT MaskChanDataAcceptorI

    //----------------------------------------HELPER METHODS
}
