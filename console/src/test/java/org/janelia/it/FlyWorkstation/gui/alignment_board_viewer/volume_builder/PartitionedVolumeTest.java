package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_builder;

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
public class PartitionedVolumeTest {
    @Before
    public void setup() {}

    @After
    public void tearDown() {}

    // todo known bug in thing being tested. @Test
    public void storeToVolume() throws Exception {
        PartitionedVolumeData pvd = PartitionedVolumeData.createCubedPartitions( 5, 5, 5, 2, 2 );
        String testValue = "AABBCCDDEEFFGGHHIIJJKKLLMMNNOOPPQQRRSSTTUUVVWWXXYY";

        String allData = testValue + testValue + testValue + testValue + testValue;
        byte[] dataBytes = allData.getBytes();

        // First set the values.
        for ( int i = 0; i < dataBytes.length; i++ ) {
            pvd.setValueAt( i, dataBytes[ i ] );
        }

        // Next check the values.
        for ( int i = 0; i < pvd.length(); i++ ) {
            byte nextByte = pvd.getValueAt( i );
            if ( nextByte != dataBytes[ i ] ) {
                Assert.fail("Data byte at " + i + " does not agree with source.");
            }
        }

        // Get the chunks.
        PartitionedVolumeData.VolumeChunk[][][] chunks = pvd.getCachedVolumeChunks();
        StringBuilder overallReturn = new StringBuilder();
        for ( int i = 0; i < pvd.getAxialDimensions()[ 2 ]; i++ ) {
            for ( int j = 0; j < pvd.getAxialDimensions()[ 1 ]; j++ ) {
                for ( int k = 0; k < pvd.getAxialDimensions()[ 0 ]; k++ ) {
                    // Look at this chunk.
                    byte[] chunkData = chunks[ i ][ j ][ k ].getVolumeData();
                    StringBuilder sb = new StringBuilder();
                    for ( int l = 0; l < chunkData.length; l++ ) {
                        sb.append( chunkData[ l ] );
                    }
                    System.out.println( sb.toString() );
                    overallReturn.append( sb.toString() );
                }
            }
        }

        if (! allData.equals( overallReturn.toString() ) ) {
            Assert.fail( "Returned data "+overallReturn.toString()+" does not equal input data " + allData );
        }

    }
}
