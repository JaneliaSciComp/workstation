package org.janelia.it.FlyWorkstation.gui.viewer3d.loader;

import org.slf4j.Logger;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/15/13
 * Time: 1:18 PM
 *
 * Implement this to accept data coming from mask and channel data.
 */
public interface MaskChanDataAcceptorI {
    public enum Acceptable {
        mask, channel, both
    }

    /**
     * Implement this if your implementation can accept "channel" data.
     * @see Acceptable
     * the caller will post channel-bytes data.  This method and
     * @see #addMaskData(Integer, long, long, long, long)
     * should be called together, as this channel data applies to a specific mask number.
     *
     * @param orignalMaskNum number of mask associated with this channel data, as it was originally read.
     * @param channelData all data applicable for the mask.
     * @param position where in the logical output would this string fall?
     * @param x logical x coordinate.
     * @param y logical y coordinate.
     * @param z logical z coordinate.
     * @param channelMetaData aids in interpretting the channel data.
     * @return number of slots filled.  Should return 1.
     * @throws Exception from called method.
     */
    int addChannelData(
        Integer orignalMaskNum, byte[] channelData, long position, long x, long y, long z, ChannelMetaData channelMetaData
    ) throws Exception;

    /**
     * Implement this if your implementation can accept "mask" data
     * @see Acceptable
     * the caller will post mask-number data.  Called once per mask.
     *
     * @param maskNumber this value is used to fill all positions between start and end.
     * @param position where in the logical output would this string fall?
     * @param x logical x coordinate.
     * @param y logical y coordinate.
     * @param z logical z coordinate.
     * @return number of slots filled.  Should return 1.
     * @throws Exception
     */
    int addMaskData( Integer maskNumber, long position, long x, long y, long z ) throws Exception;

    /**
     * This allows a poke-in of the max dimensions.
     *
     * @param x max along x
     * @param y max along y
     * @param z max along z
     * @param paddedX max along x after padding
     * @param paddedY max along y after padding
     * @param paddedZ max along z after padding
     */
    void setSpaceSize( long x, long y, long z,
                       long paddedX, long paddedY, long paddedZ, float[] coordCoverage );

    /**
     * Allows this impl to tell the caller which of the above add-mask methods are acceptable to it.
     *
     * @return can we do mask, channel, or both?
     */
    public Acceptable getAcceptableInputs();

    /**
     * Need this to calcluate how many bytes will be used per position.  Can help avoid misunderstandings between
     * caller and this impl.   Probably multiplied by number of bytes per channel.
     *
     * @return agreed-upon number of bytes per channel.
     */
    int getChannelCount();

    /**
     * Allows passing-in of various data required at channel-data read time.
     *
     * @param metaData what's to know about channels.
     */
    public void setChannelMetaData( ChannelMetaData metaData );

    /**
     * THis must be called after all data have been pushed into this acceptor.
     */
    public void endData( Logger logger );
}
