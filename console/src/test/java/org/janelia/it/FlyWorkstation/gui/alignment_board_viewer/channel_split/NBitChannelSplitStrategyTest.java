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
        System.out.println("Split 2-byte channels.");

        // Will keep only this one meta-data, but keep switching its renderable bean.
        ChannelMetaData channelMetaData = new ChannelMetaData();
        channelMetaData.byteCount = 2;
        channelMetaData.channelCount = 4;
        channelMetaData.rawChannelCount = 3;
        channelMetaData.blueChannelInx = 2;
        channelMetaData.greenChannelInx = 1;
        channelMetaData.redChannelInx = 0;

        // The beans of interest will be those numbers seen in bulding up the mask-tracker.
        Set<RenderableBean> renderables = getMockRenderableBeans();

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
        int multiMaskId = 57;
        byte[] finalChannelsData = channelSplitter.getUpdatedValue(channelMetaData, 1, channelsData, multiMaskId);
        dumpAttempt(1, channelsData, finalChannelsData, multiMaskId);

        channelsData = new byte[ channelMetaData.channelCount * channelMetaData.byteCount ];
        channelsData[ 0 ] = (byte)6;
        channelsData[ 1 ] = (byte)6;
        channelsData[ 2 ] = (byte)9;
        channelsData[ 3 ] = (byte)9;
        channelsData[ 4 ] = (byte)4;
        channelsData[ 5 ] = (byte)4;
        finalChannelsData = channelSplitter.getUpdatedValue(channelMetaData, 2, channelsData, multiMaskId);
        dumpAttempt(2, channelsData, finalChannelsData, multiMaskId);

        channelsData = new byte[ channelMetaData.channelCount * channelMetaData.byteCount ];
        channelsData[ 0 ] = (byte)0;
        channelsData[ 1 ] = (byte)3;
        channelsData[ 2 ] = (byte)25;
        channelsData[ 3 ] = (byte)7;
        channelsData[ 4 ] = (byte)121;
        channelsData[ 5 ] = (byte)0;
        finalChannelsData = channelSplitter.getUpdatedValue(channelMetaData, 4, channelsData, multiMaskId);
        dumpAttempt(4, channelsData, finalChannelsData, multiMaskId);
        System.out.println();
    }

    @Test
    public void splitOneByteChannels() throws Exception {
        System.out.println();
        System.out.println("Split 1-byte channels.");
        ChannelMetaData channelMetaData = makeOneByteChannelMetaData();
        Set<RenderableBean> renderables = getMockRenderableBeans();

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
        int multiMaskId = 57;
        int origMask = 1;
        byte[] finalChannelsData = channelSplitter.getUpdatedValue(channelMetaData, origMask, channelsData, multiMaskId);
        dumpAttempt(origMask, channelsData, finalChannelsData, multiMaskId);

        channelsData = new byte[ channelMetaData.channelCount * channelMetaData.byteCount ];
        channelsData[ 0 ] = (byte)6;
        channelsData[ 1 ] = (byte)6;
        channelsData[ 2 ] = (byte)9;
        channelsData[ 3 ] = (byte)9;
        origMask = 2;
        finalChannelsData = channelSplitter.getUpdatedValue(channelMetaData, origMask, channelsData, multiMaskId);
        dumpAttempt(origMask, channelsData, finalChannelsData, multiMaskId);

        channelsData = new byte[ channelMetaData.channelCount * channelMetaData.byteCount ];
        channelsData[ 0 ] = (byte)0;
        channelsData[ 1 ] = (byte)3;
        channelsData[ 2 ] = (byte)25;
        channelsData[ 3 ] = (byte)7;
        origMask = 4;
        finalChannelsData = channelSplitter.getUpdatedValue(channelMetaData, origMask, channelsData, multiMaskId);
        dumpAttempt(origMask, channelsData, finalChannelsData, multiMaskId);

        System.out.println();
    }

    @Test
    public void splitDepthOfFive() {
        System.out.println();
        System.out.println("-=====================Testing Split Depth of 5");
        // Here, setup with the normal boilerplate.
        ChannelMetaData channelMetaData = makeOneByteChannelMetaData();
        NBitChannelSplitStrategy channelSplitter = new NBitChannelSplitStrategy(tracker, 4);
        Set<RenderableBean> renderables = getMockRenderableBeans();
        channelMetaData.renderableBean = renderables.iterator().next();

        // Next, mock out the test scenario.
        byte[] channelsData = null;

        /*  This dump is output from the MultiMaskTrackerTest.  Using it to drive _this_ test.
        Looking at multimask 69
        12
        11
        13
        14
        15
         */
        channelsData = new byte[ channelMetaData.channelCount * channelMetaData.byteCount ];
        channelsData[ 0 ] = (byte)115;
        channelsData[ 1 ] = (byte)255;
        channelsData[ 2 ] = (byte)7;
        channelsData[ 3 ] = (byte)3;

        int multiMaskId = 69;
        addSplitData(channelMetaData, channelSplitter, channelsData, 12, multiMaskId);
        channelsData[ 1 ] = 2;
        addSplitData(channelMetaData, channelSplitter, channelsData, 11, multiMaskId);
        channelsData[ 0 ] = 17;
        channelsData[ 3 ] = 27;
        addSplitData(channelMetaData, channelSplitter, channelsData, 13, multiMaskId);
        addSplitData(channelMetaData, channelSplitter, channelsData, 14, multiMaskId);
        addSplitData(channelMetaData, channelSplitter, channelsData, 15, multiMaskId);
        System.out.println();
    }

    private void addSplitData(ChannelMetaData channelMetaData, NBitChannelSplitStrategy channelSplitter, byte[] channelsData, int origMask, int multiMaskId) {
        byte[] orableChannelPart;
        orableChannelPart = channelSplitter.getUpdatedValue( channelMetaData, origMask, channelsData, multiMaskId );
        dumpAttempt(origMask, channelsData, orableChannelPart, multiMaskId);
    }

    private Set<RenderableBean> getMockRenderableBeans() {
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
        return renderables;
    }

    private ChannelMetaData makeOneByteChannelMetaData() {
        // Will keep only this one meta-data, but keep switching its renderable bean.
        ChannelMetaData channelMetaData = new ChannelMetaData();
        channelMetaData.byteCount = 1;
        channelMetaData.channelCount = 4;
        channelMetaData.rawChannelCount = 3;
        channelMetaData.blueChannelInx = 2;
        channelMetaData.greenChannelInx = 1;
        channelMetaData.redChannelInx = 0;
        return channelMetaData;
    }

    private void dumpAttempt(int origMask, byte[] channelsData, byte[] orableChannelsData, int multiMaskId) {
        System.out.println("For channel split for incoming mask of "+origMask+" against multimask "+multiMaskId);
        System.out.print("input=");
        dumpResult(channelsData);
        System.out.print("OR-able Output=");
        dumpResult( orableChannelsData );

        MultiMaskTracker.MultiMaskBean multiMaskBean = tracker.getMultiMaskBean( multiMaskId );
        int maskOffset = multiMaskBean.getMaskOffset( origMask );
        System.out.println("Mask offset is " + maskOffset);
        System.out.println();
    }

    private void dumpResult( byte[] result ) {
        for ( int i = 0; i < result.length; i++ ) {
            System.out.print( String.format( "%02x", result[ i ] ) );
        }
        System.out.println();
        for ( int i = 0; i < result.length; i++ ) {
            System.out.print( String.format( "%d ", result[ i ] ) );
        }
        System.out.println();

    }
}
