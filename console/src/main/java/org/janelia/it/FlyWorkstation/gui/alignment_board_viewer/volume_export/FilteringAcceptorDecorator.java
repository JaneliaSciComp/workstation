package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_export;

import org.janelia.it.jacs.shared.loader.AbstractAcceptorDecorator;
import org.janelia.it.jacs.shared.loader.ChannelMetaData;
import org.janelia.it.jacs.shared.loader.MaskChanDataAcceptorI;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 4/15/13
 * Time: 4:11 PM
 *
 * Filters what winds up in the wrapped acceptor, based on crop coords.
 */
public class FilteringAcceptorDecorator extends AbstractAcceptorDecorator {
    private final Collection<float[]> cropCoordsCollection;

    public FilteringAcceptorDecorator(MaskChanDataAcceptorI wrappedAcceptor, Collection<float[]> cropCoordsCollection) {
        setWrappedAcceptor( wrappedAcceptor );
        this.cropCoordsCollection = cropCoordsCollection;
    }

    @Override
    public int addChannelData(Integer orignalMaskNum, byte[] channelData, long position, long x, long y, long z, ChannelMetaData channelMetaData) throws Exception {
        if ( wrappedAcceptor.getAcceptableInputs() != MaskChanDataAcceptorI.Acceptable.mask ) {
            for ( float[] cropCoords: cropCoordsCollection ) {
                if ( inCrop( x, y, z, cropCoords ) ) {
                    return wrappedAcceptor.addChannelData( orignalMaskNum, channelData, position, x, y, z, channelMetaData);
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
