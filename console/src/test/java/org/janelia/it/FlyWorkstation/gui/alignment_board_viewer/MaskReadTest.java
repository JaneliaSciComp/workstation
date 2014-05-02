package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer;

import org.janelia.it.jacs.compute.access.loader.renderable.RenderableBean;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_builder.RenderablesMaskBuilder;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.AlignmentBoardSettings;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.MaskChanStreamSourceI;
import org.janelia.it.jacs.compute.access.loader.MaskChanDataAcceptorI;
import org.janelia.it.jacs.compute.access.loader.MaskChanMultiFileLoader;
import org.janelia.it.jacs.model.TestCategories;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/15/13
 * Time: 11:19 AM
 *
 * This tests the read efficacy of channel files.
 */
public class MaskReadTest {

    // copied to test/resources from /nobackup/jacs/jacsData/filestore/MaskResources/Compartment/maskChannelFormatWithTemplate/compartment_34.mask
    private static final String TEST_FILE_NAME = "/MaskResources/Compartment/maskChannelFormatWithTemplate/compartment_34.mask";

    private Logger logger = LoggerFactory.getLogger( MaskReadTest.class );

    // The input data is known to be little-endian or LSB.
    private byte[] longArray = new byte[ 8 ];

    private ByteBuffer longBuffer = ByteBuffer.wrap( longArray );
    {
        longBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    @Test
    @Category(TestCategories.FastTests.class)
    public void testReadChannelData() throws Exception {
        logger.info( "Reading channel data." );
        AlignmentBoardSettings settings = new AlignmentBoardSettings();
        settings.setShowChannelData( true );
        settings.setGammaFactor(AlignmentBoardSettings.DEFAULT_GAMMA);
        settings.setChosenDownSampleRate(AlignmentBoardSettings.UNSELECTED_DOWNSAMPLE_RATE);
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
                assertNotNull("cannot find " + TEST_FILE_NAME + " for input stream", testStream);
                return new BufferedInputStream( testStream );
            }

            @Override
            public InputStream getChannelInputStream() throws IOException {
                return null;
            }
        };

        loader.read(bean, streamSource);
        assertEquals("invalid voxel count loaded", new Long(15156), bean.getVoxelCount());
    }

}
