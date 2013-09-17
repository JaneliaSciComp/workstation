package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking;

import org.junit.Before;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 9/10/13
 * Time: 11:35 AM
 *
 * This test exercises the mult-mask tracker.  It also has a util method to give other tests access to a pre-configured
 * multi-mask tracker to aid in their testing.
 */
public class MultiMaskTrackerTest {
    private MultiMaskTracker tracker;

    public static MultiMaskTracker createMultiMaskTracker() {
        // This should setup a tracker containing several multimasks.
        MultiMaskTracker tracker = new MultiMaskTracker();
        tracker.setFirstMaskNum(55);
        tracker.getMask(1, 2);
        tracker.getMask(1, 2);
        tracker.getMask(1, 2);
        tracker.getMask(1, 2);
        tracker.getMask(3, 55);
        tracker.getMask(4, 55);
        tracker.getMask(4, 55);
        tracker.getMask(4, 55);

        tracker.getMask(2, 6);
        tracker.getMask(3, 56);
        tracker.getMask(4, 57);
        // This should make a highest-priority mask.
        for ( int i = 0; i < 50; i++ ) {
            tracker.getMask(4, 7);
        }
        return tracker;
    }

    @Before
    public void setUp() throws Exception {
        tracker = createMultiMaskTracker();
    }

    @Test
    public void testTrackerContents() throws Exception {
        for ( Integer multiMask: tracker.getMultiMaskBeans().keySet() ) {
            MultiMaskTracker.MultiMaskBean bean = tracker.getMultiMaskBeans().get( multiMask );
            System.out.println("Looking at multimask " + multiMask);
            for ( Integer subMask: bean.getAltMasks() ) {
                System.out.println( subMask );
            }
            System.out.println("Mask expansion is " + tracker.getMaskExpansionCount( multiMask ) );
            System.out.println("Voxel count is " + tracker.getMultiMaskBeans().get( multiMask ).getVoxelCount() );
        }
    }

}
