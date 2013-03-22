package org.janelia.it.FlyWorkstation.gui.viewer3d.channel;

import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanDataAcceptorI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanMultiFileLoader;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.RenderablesMaskBuilder;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableBean;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/15/13
 * Time: 11:19 AM
 *
 * This tests the read efficacy of channel files.
 */
public class MaskReadTest {

    ///Users/fosterl/Documents/alignment_board/Mask_Chan
    private static final String TEST_FILE_NAME = "prefix_1.mask";
    private static final String LOCAL_FILE_PATH = "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_1.mask";

    private InputStream testStream;

    private Logger logger = LoggerFactory.getLogger( MaskReadTest.class );

    // The input data is known to be little-endian or LSB.
    private byte[] longArray = new byte[ 8 ];

    private ByteBuffer longBuffer = ByteBuffer.wrap( longArray );
    {
        longBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    @Before
    public void setUp() throws  Exception {
        testStream = this.getClass().getResourceAsStream( TEST_FILE_NAME );
        if ( testStream == null ) {
            testStream = new FileInputStream( LOCAL_FILE_PATH );
        }
    }

    @After
    public void tearDown() throws Exception {
        testStream.close();
    }

    @Test
    public void testReadChannelData() throws Exception {
        logger.info( "Reading channel data." );
        RenderablesMaskBuilder renderablesMaskBuilder = new RenderablesMaskBuilder();
        Collection<MaskChanDataAcceptorI> acceptors = new ArrayList<MaskChanDataAcceptorI>();
        acceptors.add( renderablesMaskBuilder );

        MaskChanMultiFileLoader loader = new MaskChanMultiFileLoader();
        loader.setAcceptors(acceptors);
        RenderableBean bean = new RenderableBean();
        //loader.setRenderableBeans(Arrays.asList( bean ) );

        // TODO make an alternative that takes the right stream:  see also RenMaskBldrTest.
        loader.read( bean, new BufferedInputStream( testStream ), null );
        logger.info( "Completed read-channel data." );
    }

}
