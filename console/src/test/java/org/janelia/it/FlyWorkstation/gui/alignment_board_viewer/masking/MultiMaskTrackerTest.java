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
        tracker.getMask(3, 57);
        tracker.getMask(5, 59);
        // This should make a highest-priority mask.
        for ( int i = 0; i < 50; i++ ) {
            tracker.getMask(4, 7);
        }

        tracker.getMask(6, 60);
        tracker.getMask(7, 61);
        tracker.getMask(7, 55);
        tracker.getMask(7, 62);
        tracker.getMask(8, 62);

        tracker.getMask(11,12); // Getting 66 as of last debug step-through.
        tracker.getMask(13,66);
        tracker.getMask(14,67);
        tracker.getMask(15,68); // Should have exactly 5.

        tracker.getMask(1,17);
        tracker.getMask(2,70);
        tracker.getMask(3,71);
        tracker.getMask(4,72);
        tracker.getMask(5,73);  // Should have exactly 6.

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
