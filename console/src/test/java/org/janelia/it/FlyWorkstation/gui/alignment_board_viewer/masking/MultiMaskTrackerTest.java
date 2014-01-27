package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking;

import org.junit.Assert;
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
 *
 * Rationale: this is a very important, and easily-broken facility which is directly involved in image quality.  Hence
 * it is being shaken down thoroughly, here.
 */
public class MultiMaskTrackerTest {
    private MultiMaskTracker tracker;

    private static final String EXPECTED_STRING_OUTPUT = "Looking at multimask 58.\n" +
            "6.\n" +
            "2.\n" +
            "Mask expansion is 2.\n" +
            "Voxel count is 1.\n" +
            "Looking at multimask 57.\n" +
            "2.\n" +
            "1.\n" +
            "4.\n" +
            "Mask expansion is 3.\n" +
            "Voxel count is 2.\n" +
            "Looking at multimask 56.\n" +
            "2.\n" +
            "1.\n" +
            "3.\n" +
            "Mask expansion is 3.\n" +
            "Voxel count is 1.\n" +
            "Looking at multimask 63.\n" +
            "2.\n" +
            "1.\n" +
            "4.\n" +
            "3.\n" +
            "5.\n" +
            "6.\n" +
            "7.\n" +
            "Mask expansion is 7.\n" +
            "Voxel count is 1.\n" +
            "Looking at multimask 61.\n" +
            "7.\n" +
            "4.\n" +
            "Mask expansion is 2.\n" +
            "Voxel count is 50.\n" +
            "Looking at multimask 69.\n" +
            "12.\n" +
            "11.\n" +
            "13.\n" +
            "14.\n" +
            "Mask expansion is 4.\n" +
            "Voxel count is 1.\n" +
            "Looking at multimask 70.\n" +
            "68.\n" +
            "15.\n" +
            "Mask expansion is 2.\n" +
            "Voxel count is 1.\n" +
            "Looking at multimask 64.\n" +
            "55.\n" +
            "7.\n" +
            "Mask expansion is 2.\n" +
            "Voxel count is 1.\n" +
            "Looking at multimask 66.\n" +
            "65.\n" +
            "7.\n" +
            "8.\n" +
            "Mask expansion is 3.\n" +
            "Voxel count is 1.\n" +
            "Looking at multimask 75.\n" +
            "17.\n" +
            "1.\n" +
            "2.\n" +
            "3.\n" +
            "4.\n" +
            "5.\n" +
            "Mask expansion is 6.\n" +
            "Voxel count is 1.\n";

    public static MultiMaskTracker createMultiMaskTracker() {
        // This should setup a tracker containing several multimasks.
        MultiMaskTracker tracker = new MultiMaskTracker();
        tracker.setFirstMaskNum(55);
        tracker.getMask(1, 2);        // Discovers 1, where 2 was before.  Therefore 2 is higher priority.
        tracker.getMask(1, 2);
        tracker.getMask(1, 2);
        Integer maskValue = tracker.getMask(1, 2);
        Assert.assertTrue("Mask value " + maskValue + " Not equal to expected 55", maskValue == 55);
        tracker.getMask(3, 55);
        tracker.getMask(4, 55);
        tracker.getMask(4, 55);
        maskValue = tracker.getMask(4, 55);
        Assert.assertTrue( "Mask value " + maskValue + " Not equal to expected 57", maskValue == 57 );

        maskValue = tracker.getMask(2, 6);
        Assert.assertTrue( "Mask value " + maskValue + " Not equal to expected 58", maskValue == 58 );
        tracker.getMask(3, 57);
        tracker.getMask(5, 59);
        // This should make a highest-priority mask.
        for ( int i = 0; i < 50; i++ ) {
            maskValue = tracker.getMask(4, 7);
        }
        Assert.assertTrue( "Mask value " + maskValue + " Not equal to expected 61.", maskValue == 61 );
        int maskExpansionCount = tracker.getMaskExpansionCount( maskValue );
        Assert.assertTrue(
                "Mask Value 61 does not have expected count of 2.  Instead it has " + maskExpansionCount,
                maskExpansionCount == 2
        );
        MultiMaskTracker.MultiMaskBean mmBean = tracker.getMultiMaskBean( maskValue );
        Assert.assertTrue(
                "Mask value " + maskValue + " should have voxel count of 50.  Instead it has " + mmBean.getVoxelCount(),
                mmBean.getVoxelCount() == 50
        );

        tracker.getMask(6, 60);
        tracker.getMask(7, 62);
        tracker.getMask(7, 55);
        tracker.getMask(7, 65);
        tracker.getMask(8, 65);

        tracker.getMask(11,12); // Getting 66 as of last debug step-through.
        tracker.getMask(13,67);
        tracker.getMask(14,68);
        maskValue = tracker.getMask(15,68); // Should have exactly 5.
        mmBean = tracker.getMultiMaskBean(maskValue);
        Assert.assertTrue(
                "Mask value " + maskValue + " should have a submask count of 2.  Instead it has " + maskExpansionCount,
                maskExpansionCount == 2
        );

        tracker.getMask(1,17);
        tracker.getMask(2,71);
        tracker.getMask(3,72);
        tracker.getMask(4,73);
        maskValue = tracker.getMask(5,74);  // Should have exactly 6.
        Assert.assertTrue(
                "Mask value " + maskValue + " should have a submask count of 6.",
                tracker.getMaskExpansionCount( maskValue ) == 6
        );

        return tracker;
    }

    @Before
    public void setUp() throws Exception {
        tracker = createMultiMaskTracker();
    }

    @Test
    public void testTrackerContents() throws Exception {
        StringBuilder outputBuilder = new StringBuilder();
        for ( Integer multiMask: tracker.getMultiMaskBeans().keySet() ) {
            MultiMaskTracker.MultiMaskBean bean = tracker.getMultiMaskBeans().get( multiMask );
            outputBuilder.append("Looking at multimask " + multiMask).append(".\n");
            for ( Integer subMask: bean.getAltMasks() ) {
                outputBuilder.append(subMask).append(".\n");
            }
            outputBuilder.append("Mask expansion is " + tracker.getMaskExpansionCount(multiMask)).append(".\n");
            outputBuilder.append("Voxel count is " + tracker.getMultiMaskBeans().get(multiMask).getVoxelCount()).append(".\n");
        }

        Assert.assertTrue(
                "Expected output of /" + EXPECTED_STRING_OUTPUT + "/." +
                " Instead received /" + outputBuilder.toString() + "/.",
                outputBuilder.toString().equals( EXPECTED_STRING_OUTPUT )
        );
    }

}
