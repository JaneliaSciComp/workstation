package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer;

import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.MultiMaskTracker;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable.RenderableBean;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_builder.RenderablesChannelsBuilder;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.AlignmentBoardSettings;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.MaskChanStreamSourceI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanDataAcceptorI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanMultiFileLoader;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.VolumeDataI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.volume_builder.VolumeDataChunk;
import org.janelia.it.jacs.model.TestCategories;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/14/13
 * Time: 4:47 PM
 *
 * This test will check the efficacy of the renderables mask builder.
 */
public class ChannelReadTest {

    // copied to test/resources from /nobackup/jacs/jacsData/filestore/MaskResources/Compartment/maskChannelFormatWithTemplate
    private static final String MASK_FILE_NAME = "/MaskResources/Compartment/maskChannelFormatWithTemplate/compartment_57.mask";
    private static final String CHAN_FILE_NAME = "/MaskResources/Compartment/maskChannelFormatWithTemplate/compartment_57.chan";

    @Test
    @Category(TestCategories.FastTests.class)
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
        settings.setChosenDownSampleRate(AlignmentBoardSettings.UNSELECTED_DOWNSAMPLE_RATE);
        RenderablesChannelsBuilder builder = new RenderablesChannelsBuilder( settings, new MultiMaskTracker(), null, null );
        loader.setAcceptors( Arrays.<MaskChanDataAcceptorI>asList( builder ) );

        MaskChanStreamSourceI streamSource = new MaskChanStreamSourceI() {
            @Override
            public InputStream getMaskInputStream() throws IOException {
                return getInputStream(MASK_FILE_NAME);
            }

            @Override
            public InputStream getChannelInputStream() throws IOException {
                return getInputStream(CHAN_FILE_NAME);
            }
        };
        loader.read( bean, streamSource );

        validateBuilder(builder);
    }

    private InputStream getInputStream(String path) {
        InputStream stream = this.getClass().getResourceAsStream(path);
        assertNotNull("cannot find " + path + " for input stream", stream);
        return stream;
    }

    // adapted from RenderableChannelsBuilder.test()
    private void validateBuilder(RenderablesChannelsBuilder builder) {

        VolumeDataI channelVolumeData = builder.getChannelVolumeData();
        assertNotNull("null channel volume data in builder", channelVolumeData);

        int volumeDataZeroCount = 0;
        java.util.TreeMap<Byte,Integer> frequencies = new TreeMap<Byte,Integer>();
        for (VolumeDataChunk chunk: channelVolumeData.getVolumeChunks() ) {
            for ( Byte aByte: chunk.getData() ) {
                if ( aByte == (byte)0 ) {
                    volumeDataZeroCount ++;
                }
                else {
                    Integer count = frequencies.get( aByte );
                    if ( count == null ) {
                        frequencies.put( aByte, 1 );
                    }
                    else {
                        frequencies.put( aByte, ++count );
                    }
                }
            }
        }

        final Set<Byte> frequencyKeys = frequencies.keySet();
        assertNotNull("null frequencies key set", frequencyKeys);
        assertEquals("unexpected number of frequency keys", 153, frequencyKeys.size());

        final Byte byteToCheck = new Byte("-128");
        assertEquals("invalid value returned for byte " + byteToCheck, new Integer(337), frequencies.get(byteToCheck));

        assertEquals("invalid number of volume zeros found", 536511442, volumeDataZeroCount);
    }

}
