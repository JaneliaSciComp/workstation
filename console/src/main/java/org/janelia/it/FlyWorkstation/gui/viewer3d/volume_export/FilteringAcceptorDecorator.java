package org.janelia.it.FlyWorkstation.gui.viewer3d.volume_export;

import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.ChannelMetaData;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanDataAcceptorI;
import org.slf4j.Logger;

import java.util.Collection;

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
    private Collection<float[]> cropCoordsCollection;

    public FilteringAcceptorDecorator(MaskChanDataAcceptorI wrappedAcceptor, Collection<float[]> cropCoordsCollection) {
        this.wrappedAcceptor = wrappedAcceptor;
        this.cropCoordsCollection = cropCoordsCollection;
    }

    @Override
    public int addChannelData(byte[] channelData, long position, long x, long y, long z, ChannelMetaData channelMetaData) throws Exception {
        if ( wrappedAcceptor.getAcceptableInputs() != Acceptable.mask ) {
            for ( float[] cropCoords: cropCoordsCollection ) {
                if ( inCrop( x, y, z, cropCoords ) ) {
                    return wrappedAcceptor.addChannelData( channelData, position, x, y, z, channelMetaData);
                }
            }
        }

        return 1;
    }

    @Override
    public int addMaskData(Integer maskNumber, long position, long x, long y, long z) throws Exception {
        if ( wrappedAcceptor.getAcceptableInputs() != Acceptable.channel ) {
            for ( float[] cropCoords: cropCoordsCollection ) {
                if ( inCrop( x, y, z, cropCoords ) ) {
                    return wrappedAcceptor.addMaskData( maskNumber, position, x, y, z );
                }
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

    /** Test for coordinate bounds met by x,y,z */
    private boolean inCrop( long x, long y, long z, float[] cropCoords ) {
        if ( cropCoords == null ) {
            return true;
        }
        return ( x >= cropCoords[ 0 ]  &&  x <= cropCoords[ 1 ]  &&
                 y >= cropCoords[ 2 ]  &&  y <= cropCoords[ 3 ]  &&
                 z >= cropCoords[ 4 ]  &&  z <= cropCoords[ 5 ] );
    }
}
