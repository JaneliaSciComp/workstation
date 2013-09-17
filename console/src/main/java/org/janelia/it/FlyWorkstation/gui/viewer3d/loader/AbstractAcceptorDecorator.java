package org.janelia.it.FlyWorkstation.gui.viewer3d.loader;

import org.slf4j.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 7/30/13
 * Time: 1:41 PM
 *
 * Leverage a subclass of this to change what a wrapped acceptor actually sees.
 */
public abstract class AbstractAcceptorDecorator implements MaskChanDataAcceptorI {

    protected MaskChanDataAcceptorI wrappedAcceptor; // This must be provided by impl.

    @Override
    public abstract int addChannelData(Integer maskNumber, byte[] channelData, long position, long x, long y, long z, ChannelMetaData channelMetaData) throws Exception;

    @Override
    public abstract int addMaskData(Integer maskNumber, long position, long x, long y, long z) throws Exception;

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

    public void setWrappedAcceptor( MaskChanDataAcceptorI wrappedAcceptor ) {
        this.wrappedAcceptor = wrappedAcceptor;
    }
}
