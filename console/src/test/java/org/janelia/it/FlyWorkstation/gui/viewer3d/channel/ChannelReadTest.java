package org.janelia.it.FlyWorkstation.gui.viewer3d.channel;

import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.AlignmentBoardSettings;
import org.janelia.it.FlyWorkstation.gui.viewer3d.gui_elements.AlignmentBoardControlsDialog;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanDataAcceptorI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanMultiFileLoader;
import org.janelia.it.FlyWorkstation.gui.viewer3d.volume_builder.RenderablesChannelsBuilder;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableBean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
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

    private static final String MASK_CHAN_LOC = "/Users/fosterl/Documents/alignment_board/Mask_Chan/";

    private static final String MASK_FILE_NAME = "prefix_1.mask";
    private static final String LOCAL_MASK_FILE_PATH = MASK_CHAN_LOC + MASK_FILE_NAME;

    private static final String CHAN_FILE_NAME = "prefix_1.chan";
    private static final String LOCAL_CHAN_FILE_PATH = MASK_CHAN_LOC + CHAN_FILE_NAME;

    private InputStream testMaskStream;
    private InputStream testChannelStream;

    private Logger logger = LoggerFactory.getLogger( ChannelReadTest.class );

    @Before
    public void setUp() throws  Exception {
        testMaskStream = this.getClass().getResourceAsStream( MASK_FILE_NAME );
        if ( testMaskStream == null ) {
            testMaskStream = new FileInputStream( LOCAL_MASK_FILE_PATH );
            logger.warn("Resorting to hardcoded mask path.");
        }

        testChannelStream = this.getClass().getResourceAsStream( CHAN_FILE_NAME );
        if ( testChannelStream == null ) {
            testChannelStream = new FileInputStream( LOCAL_CHAN_FILE_PATH );
            logger.warn("Resorting to hardcoded channel path.");
        }
    }

    @After
    public void tearDown() throws Exception {
        testMaskStream.close();
        testChannelStream.close();
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
        RenderablesChannelsBuilder builder = new RenderablesChannelsBuilder( settings, null );
        loader.setAcceptors( Arrays.<MaskChanDataAcceptorI>asList( builder ) );

        loader.read( bean, testMaskStream, testChannelStream );

        builder.test();
    }

}
