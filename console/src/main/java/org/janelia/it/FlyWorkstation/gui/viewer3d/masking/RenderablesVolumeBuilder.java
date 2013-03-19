package org.janelia.it.FlyWorkstation.gui.viewer3d.masking;

import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanDataAcceptorI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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

    protected long sx;
    protected long sy;
    protected long sz;

    private Logger logger = LoggerFactory.getLogger( RenderablesVolumeBuilder.class );

    //----------------------------------------CONFIGURATOR METHODS
    public abstract void init();

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
    public abstract int addChannelData(List<byte[]> channelData, long position) throws Exception;

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
    public abstract int getChannelByteCount();

    //-------------------------END:-----------IMPLEMENT MaskChanDataAcceptorI

}
