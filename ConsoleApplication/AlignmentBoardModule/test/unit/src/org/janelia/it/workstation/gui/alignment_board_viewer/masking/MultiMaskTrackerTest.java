package org.janelia.it.workstation.gui.alignment_board_viewer.masking;

import org.janelia.it.jacs.model.TestCategories;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.*;

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
@Category(TestCategories.FastTests.class)
public class MultiMaskTrackerTest {
    private org.janelia.it.workstation.gui.alignment_board_viewer.masking.MultiMaskTracker tracker;

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

    public static org.janelia.it.workstation.gui.alignment_board_viewer.masking.MultiMaskTracker createMultiMaskTracker() {
        // This should setup a tracker containing several multimasks.
        org.janelia.it.workstation.gui.alignment_board_viewer.masking.MultiMaskTracker tracker = new org.janelia.it.workstation.gui.alignment_board_viewer.masking.MultiMaskTracker();
        tracker.setFirstMaskNum(55);
        tracker.getMask(1, 2);        // Discovers 1, where 2 was before.  Therefore 2 is higher priority.
        tracker.getMask(1, 2);
        tracker.getMask(1, 2);
        Integer maskValue = tracker.getMask(1, 2);
        assertEquals("invalid mask value for (1,2)", new Integer(55), maskValue);
        tracker.getMask(3, 55);
        tracker.getMask(4, 55);
        tracker.getMask(4, 55);
        maskValue = tracker.getMask(4, 55);
        assertEquals("invalid mask value for (4,55)", new Integer(57), maskValue);

        maskValue = tracker.getMask(2, 6);
        assertEquals("invalid mask value for (2,6)", new Integer(58), maskValue);
        tracker.getMask(3, 57);
        tracker.getMask(5, 59);
        // This should make a highest-priority mask.
        for ( int i = 0; i < 50; i++ ) {
            maskValue = tracker.getMask(4, 7);
        }
        assertEquals("invalid mask value for (4,7)", new Integer(61), maskValue);
        int maskExpansionCount = tracker.getMaskExpansionCount( maskValue );
        assertEquals("invalid mask expansion count for mask value " + maskValue, 2, maskExpansionCount);
        org.janelia.it.workstation.gui.alignment_board_viewer.masking.MultiMaskTracker.MultiMaskBean mmBean = tracker.getMultiMaskBean( maskValue );
        assertEquals("invalid voxel count for mask value " + maskValue, 50, mmBean.getVoxelCount());

        tracker.getMask(6, 60);
        tracker.getMask(7, 62);
        tracker.getMask(7, 55);
        tracker.getMask(7, 65);
        tracker.getMask(8, 65);

        tracker.getMask(11,12); // Getting 66 as of last debug step-through.
        tracker.getMask(13,67);
        tracker.getMask(14,68);
        maskValue = tracker.getMask(15, 68); // Should have exactly 5.
        maskExpansionCount = tracker.getMaskExpansionCount(maskValue);
        assertEquals("invalid mask expansion count for mask value " + maskValue, 2, maskExpansionCount);

        tracker.getMask(1,17);
        tracker.getMask(2,71);
        tracker.getMask(3,72);
        tracker.getMask(4,73);
        maskValue = tracker.getMask(5, 74);  // Should have exactly 6.
        maskExpansionCount = tracker.getMaskExpansionCount(maskValue);
        assertEquals("invalid mask expansion count for mask value " + maskValue, 6, maskExpansionCount);

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
            org.janelia.it.workstation.gui.alignment_board_viewer.masking.MultiMaskTracker.MultiMaskBean bean = tracker.getMultiMaskBeans().get( multiMask );
            outputBuilder.append("Looking at multimask ").append(multiMask).append(".\n");
            for ( Integer subMask: bean.getAltMasks() ) {
                outputBuilder.append(subMask).append(".\n");
            }
            outputBuilder.append("Mask expansion is ").append(tracker.getMaskExpansionCount(multiMask)).append(".\n");
            outputBuilder.append("Voxel count is ").append(tracker.getMultiMaskBeans().get(multiMask).getVoxelCount()).append(".\n");
        }

        assertTrue(
                "Expected output of /" + EXPECTED_STRING_OUTPUT + "/." +
                        " Instead received /" + outputBuilder.toString() + "/.",
                outputBuilder.toString().equals(EXPECTED_STRING_OUTPUT)
        );
    }

}
