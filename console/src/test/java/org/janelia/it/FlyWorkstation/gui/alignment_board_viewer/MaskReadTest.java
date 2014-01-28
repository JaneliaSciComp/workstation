package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer;

import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.AlignmentBoardSettings;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.gui_elements.AlignmentBoardControlsDialog;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.MaskChanStreamSourceI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanDataAcceptorI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanMultiFileLoader;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_builder.RenderablesMaskBuilder;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable.RenderableBean;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
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
    private static final String TEST_FILE_NAME = "compartment_34.mask";
    private static final String LOCAL_FILE_PATH = "/Volumes/jacsData/MaskResources/Compartment/maskChannelFormatWithTemplate/compartment_34.mask";

    private Logger logger = LoggerFactory.getLogger( MaskReadTest.class );

    // The input data is known to be little-endian or LSB.
    private byte[] longArray = new byte[ 8 ];

    private ByteBuffer longBuffer = ByteBuffer.wrap( longArray );
    {
        longBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    @Before
    public void setUp() throws  Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testReadChannelData() throws Exception {
        logger.info( "Reading channel data." );
        AlignmentBoardSettings settings = new AlignmentBoardSettings();
        settings.setShowChannelData( true );
        settings.setGammaFactor(AlignmentBoardSettings.DEFAULT_GAMMA);
        settings.setChosenDownSampleRate(AlignmentBoardControlsDialog.UNSELECTED_DOWNSAMPLE_RATE);
        settings.setDownSampleGuess( 2.0f );
        RenderableBean bean = new RenderableBean();
        RenderablesMaskBuilder renderablesMaskBuilder = new RenderablesMaskBuilder( settings, Arrays.asList(bean) );
        Collection<MaskChanDataAcceptorI> acceptors = new ArrayList<MaskChanDataAcceptorI>();
        acceptors.add( renderablesMaskBuilder );

        MaskChanMultiFileLoader loader = new MaskChanMultiFileLoader();
        loader.setAcceptors(acceptors);

        MaskChanStreamSourceI streamSource = new MaskChanStreamSourceI() {
            @Override
            public InputStream getMaskInputStream() throws IOException {
                InputStream testStream = this.getClass().getResourceAsStream( TEST_FILE_NAME );
                if ( testStream == null ) {
                    testStream = new FileInputStream( LOCAL_FILE_PATH );
                }
                return new BufferedInputStream( testStream );
            }

            @Override
            public InputStream getChannelInputStream() throws IOException {
                return null;
            }
        };

        loader.read( bean, streamSource );
        logger.info( "Completed read-channel data." );
    }

}
