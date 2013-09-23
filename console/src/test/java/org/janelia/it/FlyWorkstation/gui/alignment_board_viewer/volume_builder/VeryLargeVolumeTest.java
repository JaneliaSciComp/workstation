package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_builder;

import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.VolumeDataI;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 9/21/13
 * Time: 12:55 AM
 *
 * Test volume partitioning.  Test for exceptions resulting from invalid calculations, check for proper segmentation
 * of the data.
 */
public class VeryLargeVolumeTest {
    private VolumeDataI volumeData;
    private byte[] dataBytes;

    @Before
    public void setup() {
    }

    private void init( int slabCount ) {
        volumeData = new VeryLargeVolumeData(5,5,5, 2, slabCount);
        String testValue = "AABBCCDDEEFFGGHHIIJJKKLLMMNNOOPPQQRRSSTTUUVVWWXXYY";

        String allData = testValue + testValue + testValue + testValue + testValue;
        dataBytes = allData.getBytes();
    }

    @After
    public void tearDown() {}

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
                Assert.fail("Data byte at " + i + " does not agree with source.");
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
                int j = 6;
            }
            byte nextByte = volumeData.getValueAt( i );
            if ( nextByte != dataBytes[ i ] ) {
                Assert.fail("Data byte at " + i + " does not agree with source.");
            }
        }

    }
}
