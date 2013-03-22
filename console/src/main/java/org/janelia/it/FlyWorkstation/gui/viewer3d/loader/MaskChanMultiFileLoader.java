package org.janelia.it.FlyWorkstation.gui.viewer3d.loader;

import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
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
public class MaskChanMultiFileLoader {

    private Collection<MaskChanDataAcceptorI> maskAcceptors;
    private Collection<MaskChanDataAcceptorI> channelAcceptors;

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
        logger.info("Read called.");
        MaskChanSingleFileLoader singleFileLoader =
                new MaskChanSingleFileLoader( maskAcceptors, channelAcceptors, bean );
        singleFileLoader.read( maskInputStream, channelStream );
        logger.debug( "Read complete." );
    }

}
