package org.janelia.it.FlyWorkstation.gui.alignment_board.loader;

import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableBean;

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
    public RenderableBean renderableBean;  // Optional bean associated with this meta data.
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

    @Override
    public ChannelMetaData clone() {
        ChannelMetaData rtnVal = new ChannelMetaData();
        rtnVal.byteCount = this.byteCount;
        rtnVal.channelCount = this.channelCount;
        rtnVal.rawChannelCount = this.rawChannelCount;
        rtnVal.blueChannelInx = this.blueChannelInx;
        rtnVal.redChannelInx = this.redChannelInx;
        rtnVal.greenChannelInx = this.greenChannelInx;
        rtnVal.renderableBean = this.renderableBean;
        rtnVal.orderedRgbIndexes = this.orderedRgbIndexes;
        if ( this.orderedRgbIndexes != null ) {
            rtnVal.orderedRgbIndexes = new int[ this.orderedRgbIndexes.length ];
            for ( int i = 0; i < orderedRgbIndexes.length; i++ ) {
                rtnVal.orderedRgbIndexes[ i ] = this.orderedRgbIndexes[ i ];
            }
        }

        return rtnVal;
    }

    @Override
    public boolean equals( Object other ) {
        if ( other == null || (! ( other instanceof ChannelMetaData ) ) ) {
            return false;
        }
        else {
            ChannelMetaData otherCmd = (ChannelMetaData)other;
            return otherCmd.byteCount == this.byteCount  &&
                   otherCmd.greenChannelInx == this.greenChannelInx  &&
                   otherCmd.blueChannelInx == this.blueChannelInx  &&
                   otherCmd.redChannelInx == this.redChannelInx  &&
                   otherCmd.channelCount == this.channelCount  &&
                   otherCmd.rawChannelCount == this.rawChannelCount  &&
                   otherCmd.orderedRgbIndexes.length == this.orderedRgbIndexes.length  &&
                   otherCmd.renderableBean.equals( this.renderableBean );
        }
    }

    @Override
    public int hashCode() {
        return renderableBean.hashCode();
    }

}

