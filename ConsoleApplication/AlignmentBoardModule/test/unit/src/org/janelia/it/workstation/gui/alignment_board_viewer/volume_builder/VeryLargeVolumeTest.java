package org.janelia.it.workstation.gui.alignment_board_viewer.volume_builder;

import org.janelia.it.workstation.gui.viewer3d.masking.VolumeDataI;
import org.janelia.it.jacs.model.TestCategories;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 9/21/13
 * Time: 12:55 AM
 *
 * Test volume partitioning.  Test for exceptions resulting from invalid calculations, check for proper segmentation
 * of the data.
 */
@Category(TestCategories.FastTests.class)
public class VeryLargeVolumeTest {
    private VolumeDataI volumeData;
    private byte[] dataBytes;

    private void init( int slabCount ) {
        volumeData = new VeryLargeVolumeData(5,5,5, 2, slabCount);
        String testValue = "AABBCCDDEEFFGGHHIIJJKKLLMMNNOOPPQQRRSSTTUUVVWWXXYY";

        String allData = testValue + testValue + testValue + testValue + testValue;
        dataBytes = allData.getBytes();
    }

    @Test
    public void justOneSlab() throws Exception {
        init( 1 );

        // First set the values.
        for ( int i = 0; i < dataBytes.length; i++ ) {
            volumeData.setValueAt(i, dataBytes[i]);
        }

        // Next check the values.
        for ( int i = 0; i < volumeData.length(); i++ ) {
            byte nextByte = volumeData.getValueAt( i );
            if ( nextByte != dataBytes[ i ] ) {
                fail("Data byte at " + i + " does not agree with source.");
            }
        }

    }

    @Test
    public void multiSlab() throws Exception {
        init( 5 );

        // First set the values.
        for ( int i = 0; i < dataBytes.length; i++ ) {
            volumeData.setValueAt(i, dataBytes[i]);
        }

        // Next check the values.
        for ( int i = 0; i < volumeData.length(); i++ ) {
            if ( i == 175 ) {
                // This is a marker for BP during test, in debugger.
                @SuppressWarnings("UnusedDeclaration")
                int j = 6;
            }
            byte nextByte = volumeData.getValueAt( i );
            if ( nextByte != dataBytes[ i ] ) {
                fail("Data byte at " + i + " does not agree with source.");
            }
        }

    }
}
