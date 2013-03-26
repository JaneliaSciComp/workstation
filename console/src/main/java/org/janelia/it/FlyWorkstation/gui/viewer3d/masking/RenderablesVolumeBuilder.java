package org.janelia.it.FlyWorkstation.gui.viewer3d.masking;

import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanDataAcceptorI;


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
    protected float[] coordCoverage;

    //----------------------------------------CONFIGURATOR METHODS
    public abstract void init();

    //----------------------------------------IMPLEMENT MaskChanDataAcceptorI (partially)
    @Override
    public void setSpaceSize( long x, long y, long z, float[] coordCoverage ) {
        sx = x;
        sy = y;
        sz = z;
        this.coordCoverage = coordCoverage;
    }

    //-------------------------END:-----------IMPLEMENT MaskChanDataAcceptorI

}
