package org.janelia.it.FlyWorkstation.gui.viewer3d.loader;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/19/13
 * Time: 12:52 PM
 *
 * This bean-like class contains various information about the channel.  It should be provided to "acceptors" of
 * channel data.
 */
public class ChannelMetaData {
    public int byteCount = -1;
    public int channelCount = -1;
    public int rawChannelCount = -1;
    public int blueChannelInx = -1;
    public int greenChannelInx = -1;
    public int redChannelInx = -1;
    private int[] orderedRgbIndexes;

    public int[] getOrderedRgbIndexes() {
        if ( orderedRgbIndexes == null ) {
            orderedRgbIndexes = new int[ 3 ];
            orderedRgbIndexes[ 0 ] = redChannelInx;
            orderedRgbIndexes[ 1 ] = greenChannelInx;
            orderedRgbIndexes[ 2 ] = blueChannelInx;
        }
        return orderedRgbIndexes;
    }
}

