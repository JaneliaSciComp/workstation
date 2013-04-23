package org.janelia.it.FlyWorkstation.gui.viewer3d.volume_export;

import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.ChannelMetaData;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanDataAcceptorI;
import org.slf4j.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 4/15/13
 * Time: 4:11 PM
 *
 * Filters what winds up in the wrapped acceptor, based on crop coords.
 */
public class FilteringAcceptorDecorator implements MaskChanDataAcceptorI {
    private MaskChanDataAcceptorI wrappedAcceptor;
    private float[] cropCoords;

    public FilteringAcceptorDecorator(MaskChanDataAcceptorI wrappedAcceptor, float[] cropCoords) {
        this.wrappedAcceptor = wrappedAcceptor;
        this.cropCoords = cropCoords;
    }

    @Override
    public int addChannelData(byte[] channelData, long position, long x, long y, long z) throws Exception {
        if ( wrappedAcceptor.getAcceptableInputs() != Acceptable.mask ) {
            if ( x >= cropCoords[ 0 ]  &&  x <= cropCoords[ 1 ]  &&
                    y >= cropCoords[ 2 ]  &&  y <= cropCoords[ 3 ]  &&
                    z >= cropCoords[ 4 ]  &&  z <= cropCoords[ 5 ] ) {

                return wrappedAcceptor.addChannelData( channelData, position, x, y, z );
            }
        }

        return 1;
    }

    @Override
    public int addMaskData(Integer maskNumber, long position, long x, long y, long z) throws Exception {
        if ( wrappedAcceptor.getAcceptableInputs() != Acceptable.channel ) {
            if ( x >= cropCoords[ 0 ]  &&  x <= cropCoords[ 1 ]  &&
                    y >= cropCoords[ 2 ]  &&  y <= cropCoords[ 3 ]  &&
                    z >= cropCoords[ 4 ]  &&  z <= cropCoords[ 5 ] ) {

                return wrappedAcceptor.addMaskData( maskNumber, position, x, y, z );
            }
        }

        return 1;
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
