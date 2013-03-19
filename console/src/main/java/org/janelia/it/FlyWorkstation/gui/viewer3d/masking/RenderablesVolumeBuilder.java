package org.janelia.it.FlyWorkstation.gui.viewer3d.masking;

import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanDataAcceptorI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/13/13
 * Time: 4:21 PM
 *
 * This implementation of a mask builder takes renderables as its driving data.  It will accept the renderables,
 * along with their applicable chunks of data, to produce its texture data volume, in memory.
 */
public abstract class RenderablesVolumeBuilder implements MaskChanDataAcceptorI {

    private byte[] volumeData;
    private long sx;
    private long sy;
    private long sz;

    private int byteCount = 0;
    private int channelCount = 1;

    private Logger logger = LoggerFactory.getLogger( RenderablesVolumeBuilder.class );

    //----------------------------------------CONFIGURATOR METHODS/C'TORs
    public void setByteCount( int byteCount ) {
        this.byteCount = byteCount;
    }

    public void setChannelCount( int channelCount ) {
        this.channelCount = channelCount;
    }

    /** Call this prior to any update-data operations. */
    public void init() {
        long arrayLength = sx * sy * sz * byteCount;
        if ( arrayLength > Integer.MAX_VALUE ) {
            throw new IllegalArgumentException(
                    "Total length of input: " + arrayLength  +
                            " exceeds maximum array size capacity.  If this is truly required, code redesign will be necessary."
            );
        }

        if ( sx > Integer.MAX_VALUE || sy > Integer.MAX_VALUE || sz > Integer.MAX_VALUE ) {
            throw new IllegalArgumentException(
                    "One or more of the axial lengths (" + sx + "," + sy + "," + sz +
                            ") exceeds max value for an integer.  If this is truly required, code redesign will be necessary."
            );
        }

        volumeData = new byte[ (int) arrayLength ];
    }

    public byte[] getVolumeData() {
        return volumeData;
    }

    public int getByteCount() {
        return byteCount;
    }

    //----------------------------------------IMPLEMENT MaskChanDataAcceptorI (partially)
    @Override
    public void setSpaceSize( long x, long y, long z ) {
        sx = x;
        sy = y;
        sz = z;
    }

    /**
     * This is called with data to be loaded.
     *
     * @param maskNumber describes all points belonging to all pairs.
     * @param position where in the linear volume coords does this go?
     * @return total bytes read during this pairs-run.
     * @throws Exception thrown by called methods or if bad inputs are received.
     */
    @Override
    public abstract int addMaskData(Integer maskNumber, long position) throws Exception;

    @Override
    public abstract int addChannelData(byte[] channelData, long position) throws Exception;

    /**
     * This tells the caller: only call me with mask data.  This is a mask builder.
     * @return mask
     */
    @Override
    public abstract Acceptable getAcceptableInputs();

    /**
     * I think channel count is the correct number.
     * @return the channel count as established.
     */
    @Override
    public int getChannelByteCount() {
        return channelCount;
    }

    //-------------------------END:-----------IMPLEMENT MaskChanDataAcceptorI

}
