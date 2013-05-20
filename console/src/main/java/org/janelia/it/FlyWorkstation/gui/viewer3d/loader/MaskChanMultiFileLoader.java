package org.janelia.it.FlyWorkstation.gui.viewer3d.loader;

import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.VolumeConsistencyChecker;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/13/13
 * Time: 4:21 PM
 *
 * This implementation of a mask builder takes renderables as its driving data.  It will accept the renderables,
 * along with their applicable chunks of data, to produce its texture data volume, in memory.
 */
public class MaskChanMultiFileLoader {

    private boolean checkForConsistency = true;

    private Collection<MaskChanDataAcceptorI> maskAcceptors;
    private Collection<MaskChanDataAcceptorI> channelAcceptors;

    private VolumeConsistencyChecker checker = new VolumeConsistencyChecker();
    private boolean enforcePadding;
    private boolean dimWriteback;

    private Logger logger = LoggerFactory.getLogger( MaskChanMultiFileLoader.class );

    /** Anything on this list could receive data from the files under study. */
    public void setAcceptors( Collection<MaskChanDataAcceptorI> acceptors ) {
        maskAcceptors = new ArrayList<MaskChanDataAcceptorI>();
        channelAcceptors = new ArrayList<MaskChanDataAcceptorI>();

        for ( MaskChanDataAcceptorI acceptor: acceptors ) {
            if ( acceptor.getAcceptableInputs().equals( MaskChanDataAcceptorI.Acceptable.channel ) ) {
                channelAcceptors.add( acceptor );
            }
            if ( acceptor.getAcceptableInputs().equals( MaskChanDataAcceptorI.Acceptable.mask ) ) {
                maskAcceptors.add( acceptor );
            }
            if ( acceptor.getAcceptableInputs().equals( MaskChanDataAcceptorI.Acceptable.both ) ) {
                channelAcceptors.add( acceptor );
                maskAcceptors.add( acceptor );
            }
        }
    }

    /**
     * This is the master-method that tells all the single-file-loaders to read their data into the common volume.
     *
     * @param bean info read is applicable to this
     * @param maskInputStream this has the compression/ray data.
     * @param channelStream this has the channel or intensity data.
     * @throws Exception thrown by called methods.
     */
    public void read( RenderableBean bean, InputStream maskInputStream, InputStream channelStream )
            throws Exception {
        logger.debug( "Read called." );
        MaskChanSingleFileLoader singleFileLoader =
                new MaskChanSingleFileLoader( maskAcceptors, channelAcceptors, bean );

        // Here, may override the pad-out to ensure resulting volume exactly matches the original space.
        if ( ! enforcePadding ) {
            singleFileLoader.setAxialLengthDivisibility( 1 );
        }
        if ( dimWriteback ) {
            singleFileLoader.setIntensityDivisor(10);
        }
        singleFileLoader.read( maskInputStream, channelStream );

        // Accumulate information for final sanity check.
        if ( isCheckForConsistency() ) {
            checker.accumulate(
                bean.getTranslatedNum(), singleFileLoader.getDimensions(), singleFileLoader.getChannelMetaData()
            );
        }

        logger.debug( "Read complete." );
    }

    /**
     * Call this after all reading has been completed.
     */
    public void close() {
        if ( isCheckForConsistency() ) {
            checker.report( true, logger );
        }
        for ( MaskChanDataAcceptorI acceptor: maskAcceptors ) {
            acceptor.endData( logger );
        }
        for ( MaskChanDataAcceptorI acceptor: channelAcceptors ) {
            acceptor.endData( logger );
        }
    }

    public void setEnforcePadding(boolean enforcePadding) {
        this.enforcePadding = enforcePadding;
    }

    public boolean isCheckForConsistency() {
        return checkForConsistency;
    }

    public void setCheckForConsistency(boolean checkForConsistency) {
        this.checkForConsistency = checkForConsistency;
    }

    public void setDimWriteback(boolean dimWriteback) {
        this.dimWriteback = dimWriteback;
    }
}
