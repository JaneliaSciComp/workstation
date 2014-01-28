package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer;

import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.MultiMaskTracker;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.AlignmentBoardSettings;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.gui_elements.AlignmentBoardControlsDialog;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.MaskChanStreamSource;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.MaskChanStreamSourceI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanDataAcceptorI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanMultiFileLoader;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_builder.RenderablesChannelsBuilder;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable.RenderableBean;

import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.VolumeDataI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.FileResolver;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.TrivialFileResolver;
import org.janelia.it.FlyWorkstation.gui.viewer3d.volume_builder.VolumeDataChunk;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/14/13
 * Time: 4:47 PM
 *
 * This test will check the efficacy of the renderables mask builder.
 */
public class ChannelReadTest {

    private static final String MASK_CHAN_LOC = "/Volumes/jacsData/MaskResources/Compartment/maskChannelFormatWithTemplate/";

    private static final String MASK_FILE_NAME = "compartment_57.mask";
    private static final String LOCAL_MASK_FILE_PATH = MASK_CHAN_LOC + MASK_FILE_NAME;

    private static final String CHAN_FILE_NAME = "compartment_57.chan";
    private static final String LOCAL_CHAN_FILE_PATH = MASK_CHAN_LOC + CHAN_FILE_NAME;

    private Logger logger = LoggerFactory.getLogger( ChannelReadTest.class );

    @Before
    public void setUp() throws  Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testReadOneFile() throws Exception {
        // Time-of-writing: only thing bean is used for is its tanslated number.
        RenderableBean bean = new RenderableBean();
        bean.setTranslatedNum( 1 );

        MaskChanMultiFileLoader loader = new MaskChanMultiFileLoader();
        //loader.setByteCount( 2 );
        //loader.setRenderableBeans( Arrays.asList( bean ) );

        AlignmentBoardSettings settings = new AlignmentBoardSettings();
        settings.setShowChannelData( true );
        settings.setGammaFactor( AlignmentBoardSettings.DEFAULT_GAMMA );
        settings.setChosenDownSampleRate(AlignmentBoardControlsDialog.UNSELECTED_DOWNSAMPLE_RATE);
        RenderablesChannelsBuilder builder = new RenderablesChannelsBuilder( settings, new MultiMaskTracker(), null, null );
        loader.setAcceptors( Arrays.<MaskChanDataAcceptorI>asList( builder ) );

        MaskChanStreamSourceI streamSource = new MaskChanStreamSourceI() {
            @Override
            public InputStream getMaskInputStream() throws IOException {
                InputStream testMaskStream = this.getClass().getResourceAsStream( MASK_FILE_NAME );
                if ( testMaskStream == null ) {
                    testMaskStream = new FileInputStream( LOCAL_MASK_FILE_PATH );
                    logger.warn("Resorting to hardcoded mask path.");
                }

                return testMaskStream;

            }

            @Override
            public InputStream getChannelInputStream() throws IOException {
                InputStream testChannelStream = this.getClass().getResourceAsStream( CHAN_FILE_NAME );
                if ( testChannelStream == null ) {
                    testChannelStream = new FileInputStream( LOCAL_CHAN_FILE_PATH );
                    logger.warn("Resorting to hardcoded channel path.");
                }

                return testChannelStream;
            }
        };
        loader.read( bean, streamSource );

        builder.test();
    }

}
