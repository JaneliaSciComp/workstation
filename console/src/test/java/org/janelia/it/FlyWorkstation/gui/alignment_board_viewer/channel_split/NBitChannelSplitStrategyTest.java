package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.channel_split;

import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.MultiMaskTracker;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.MultiMaskTrackerTest;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable.RenderableBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.ChannelMetaData;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 9/10/13
 * Time: 11:14 AM
 *
 * Testing the channel split mechanism, for a set number of bits, less than 8.
 */
public class NBitChannelSplitStrategyTest {

    private MultiMaskTracker tracker;

    @Before
    public void setUp() throws Exception {
        tracker = MultiMaskTrackerTest.createMultiMaskTracker();
    }

    @Test
    public void splitTwoByteChannels() throws Exception {
        // Will keep only this one meta-data, but keep switching its renderable bean.
        ChannelMetaData channelMetaData = new ChannelMetaData();
        channelMetaData.byteCount = 2;
        channelMetaData.channelCount = 4;
        channelMetaData.rawChannelCount = 3;
        channelMetaData.blueChannelInx = 2;
        channelMetaData.greenChannelInx = 1;
        channelMetaData.redChannelInx = 0;

        // The beans of interest will be those numbers seen in bulding up the mask-tracker.
        Set<Integer> masks = new HashSet<Integer>();
        for ( MultiMaskTracker.MultiMaskBean bean: tracker.getMultiMaskBeans().values() ) {
            masks.addAll( bean.getAltMasks() );
        }

        Set<RenderableBean> renderables = new HashSet<RenderableBean>();
        for ( Integer altMask: masks ) {
            Long randomCount = (long)(new Random( System.currentTimeMillis() ).nextInt( 8 ));
            RenderableBean newRenderable = new RenderableBean();
            newRenderable.setVoxelCount( randomCount );
            newRenderable.setLabelFileNum( altMask );
            newRenderable.setTranslatedNum( altMask );
            newRenderable.setType( "Neuron Fragment" );

            renderables.add(newRenderable);
        }

        // Finally, traverse input values to see how the outputs look.
        NBitChannelSplitStrategy channelSplitter = new NBitChannelSplitStrategy(tracker, 4);
        channelMetaData.renderableBean = renderables.iterator().next();
        // Channels-data is little endian.  Plugging in values with that assumption.
        byte[] channelsData = null;

        channelsData = new byte[ channelMetaData.channelCount * channelMetaData.byteCount ];
        channelsData[ 0 ] = (byte)115;
        channelsData[ 1 ] = (byte)255;
        channelsData[ 2 ] = (byte)7;
        channelsData[ 3 ] = (byte)3;
        channelsData[ 4 ] = (byte)121;
        channelsData[ 5 ] = (byte)0;
        byte[] finalChannelsData = channelSplitter.getUpdatedValue(channelMetaData, 1, channelsData, 57);
        dumpAttempt(1, channelsData, finalChannelsData);

        channelsData = new byte[ channelMetaData.channelCount * channelMetaData.byteCount ];
        channelsData[ 0 ] = (byte)6;
        channelsData[ 1 ] = (byte)6;
        channelsData[ 2 ] = (byte)9;
        channelsData[ 3 ] = (byte)9;
        channelsData[ 4 ] = (byte)4;
        channelsData[ 5 ] = (byte)4;
        finalChannelsData = channelSplitter.getUpdatedValue(channelMetaData, 2, channelsData, 57);
        dumpAttempt(2, channelsData, finalChannelsData);

        channelsData = new byte[ channelMetaData.channelCount * channelMetaData.byteCount ];
        channelsData[ 0 ] = (byte)0;
        channelsData[ 1 ] = (byte)3;
        channelsData[ 2 ] = (byte)25;
        channelsData[ 3 ] = (byte)7;
        channelsData[ 4 ] = (byte)121;
        channelsData[ 5 ] = (byte)0;
        finalChannelsData = channelSplitter.getUpdatedValue(channelMetaData, 4, channelsData, 57);
        dumpAttempt(4, channelsData, finalChannelsData);
    }

    @Test
    public void splitOneByteChannels() throws Exception {
        // Will keep only this one meta-data, but keep switching its renderable bean.
        ChannelMetaData channelMetaData = new ChannelMetaData();
        channelMetaData.byteCount = 1;
        channelMetaData.channelCount = 4;
        channelMetaData.rawChannelCount = 3;
        channelMetaData.blueChannelInx = 2;
        channelMetaData.greenChannelInx = 1;
        channelMetaData.redChannelInx = 0;

        // The beans of interest will be those numbers seen in bulding up the mask-tracker.
        Set<Integer> masks = new HashSet<Integer>();
        for ( MultiMaskTracker.MultiMaskBean bean: tracker.getMultiMaskBeans().values() ) {
            masks.addAll( bean.getAltMasks() );
        }

        Set<RenderableBean> renderables = new HashSet<RenderableBean>();
        for ( Integer altMask: masks ) {
            Long randomCount = (long)(new Random( System.currentTimeMillis() ).nextInt( 8 ));
            RenderableBean newRenderable = new RenderableBean();
            newRenderable.setVoxelCount( randomCount );
            newRenderable.setLabelFileNum( altMask );
            newRenderable.setTranslatedNum( altMask );
            newRenderable.setType( "Neuron Fragment" );

            renderables.add(newRenderable);
        }

        // Finally, traverse input values to see how the outputs look.
        NBitChannelSplitStrategy channelSplitter = new NBitChannelSplitStrategy(tracker, 4);
        channelMetaData.renderableBean = renderables.iterator().next();
        // Channels-data is little endian.  Plugging in values with that assumption.
        byte[] channelsData = null;

        channelsData = new byte[ channelMetaData.channelCount * channelMetaData.byteCount ];
        channelsData[ 0 ] = (byte)115;
        channelsData[ 1 ] = (byte)255;
        channelsData[ 2 ] = (byte)7;
        channelsData[ 3 ] = (byte)3;
        byte[] finalChannelsData = channelSplitter.getUpdatedValue(channelMetaData, 1, channelsData, 57);
        dumpAttempt(1, channelsData, finalChannelsData);

        channelsData = new byte[ channelMetaData.channelCount * channelMetaData.byteCount ];
        channelsData[ 0 ] = (byte)6;
        channelsData[ 1 ] = (byte)6;
        channelsData[ 2 ] = (byte)9;
        channelsData[ 3 ] = (byte)9;
        finalChannelsData = channelSplitter.getUpdatedValue(channelMetaData, 2, channelsData, 57);
        dumpAttempt(2, channelsData, finalChannelsData);

        channelsData = new byte[ channelMetaData.channelCount * channelMetaData.byteCount ];
        channelsData[ 0 ] = (byte)0;
        channelsData[ 1 ] = (byte)3;
        channelsData[ 2 ] = (byte)25;
        channelsData[ 3 ] = (byte)7;
        finalChannelsData = channelSplitter.getUpdatedValue(channelMetaData, 4, channelsData, 57);
        dumpAttempt(4, channelsData, finalChannelsData);
    }

    private void dumpAttempt(int altMask, byte[] channelsData, byte[] finalChannelsData) {
        System.out.println("For channel split "+altMask+" of "+57);
        System.out.print("input=");
        dumpResult(channelsData);
        System.out.print("output=");
        dumpResult( finalChannelsData );
    }

    private void dumpResult( byte[] result ) {
        for ( int i = 0; i < result.length; i++ ) {
            System.out.print( String.format( "%02x", result[ i ] ) );
        }
        System.out.println();
    }
}
