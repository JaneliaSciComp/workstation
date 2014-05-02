package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_export;

import org.janelia.it.jacs.compute.access.loader.ChannelMetaData;
import org.janelia.it.jacs.compute.access.loader.MaskChanDataAcceptorI;
import org.janelia.it.jacs.model.TestCategories;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests the {@link FilteringAcceptorDecorator} class.
 *
 * @author Les Foster
 */
@Category(TestCategories.FastTests.class)
public class FilteringAcceptorDecoratorTest {

    private MaskChanDataAcceptorI wrappedAcceptor;
    private Collection<float[]> cropCoords;

    @Before
    public void setUp() {
        wrappedAcceptor = new MockAcceptor();
        cropCoords = new ArrayList<float[]>();

        // Establishing single-lines of acceptable coords, starting at origin.  Like coord axes.
        // During test, any coord that does not consist of a multiple of twenty in one of the three coords,
        // and zeros in the other two, is thus wrong.
        int extentMultiplier = 20;
        for ( int i = 0; i < 5; i++ ) {
            int nextExtent = (i+1) * extentMultiplier;
            cropCoords.add( new float[] {
                    0, 0, nextExtent,
                    0, 0, nextExtent
            });
            cropCoords.add( new float[] {
                    0, nextExtent, 0,
                    0, nextExtent, 0
            });
            cropCoords.add( new float[] {
                    nextExtent, 0, 0,
                    nextExtent, 0, 0
            });
        }
    }

    @Test
    public void testAddChannelData() throws Exception {
        ChannelMetaData md = MockAcceptor.getTestableMetaData();
        FilteringAcceptorDecorator filter = new FilteringAcceptorDecorator( wrappedAcceptor, cropCoords );
        createChannelAcceptor(md, filter);
        verifyFiteredValues();
    }

    @Test
    public void testAddMaskData() throws Exception {
        FilteringAcceptorDecorator filter = new FilteringAcceptorDecorator( wrappedAcceptor, cropCoords );
        createMaskAcceptor(filter);
        verifyFiteredValues();
    }

    @Test
    public void verifyConversions() throws Exception {
        // Simple test-tester.
        long[] originalCoords = new long[] { 25, 25, 25 };
        long[] xyz = breakDownToCoords(
                MockAcceptor.calculateLinearOffset(originalCoords[0], originalCoords[1], originalCoords[2])
        );

        for ( int i = 0; i < 3; i++ ) {
            assertEquals( originalCoords[i], xyz[i] );
        }

        originalCoords = new long[] { 5, 10, 15 };
        xyz = breakDownToCoords(
                MockAcceptor.calculateLinearOffset(originalCoords[0], originalCoords[1], originalCoords[2])
        );

        for ( int i = 0; i < 3; i++ ) {
            assertEquals( originalCoords[i], xyz[i] );
        }

    }

    private void verifyFiteredValues() {
        // Verify.
        List<Long> occupiedPositions = ((MockAcceptor)wrappedAcceptor).getOccupiedPositions();
        for ( Long nextPos: occupiedPositions ) {
            long[] coords = breakDownToCoords(nextPos);

            int nonZeroCount = 0;
            for ( int i = 0; i < 3; i++ ) {
                if ( coords[ i ] > 0 ) {
                    nonZeroCount ++;
                }
            }
            assertEquals(
                    "Exactly one non-zero axes should exist.  " +
                            coords[0] + ", " + coords[1] + ", " + coords[2]
                    , nonZeroCount, 1
            );

            for ( int i = 0; i < 3; i++ ) {
                assertEquals(
                        "Only coord-multiples of 20 should be returned. See coord " + i + " at " + coords[i],
                        coords[i] % 20, 0
                );
            }
        }
    }

    private void createChannelAcceptor(ChannelMetaData md, FilteringAcceptorDecorator filter) throws Exception {
        byte[] junkData = new byte[] { 12, 12, 12 };
        for ( int i = 0; i < 300; i++ ) {
            for ( int j = 0; j < 300; j++ ) {
                for ( int k = 0; k < 300; k++ ) {
                    //(Integer orignalMaskNum, byte[] channelData, long position, long x, long y, long z, ChannelMetaData channelMetaData)
                    long pos = MockAcceptor.X_EXTENT * i + MockAcceptor.Y_EXTENT * j + MockAcceptor.Z_EXTENT * k;
                    filter.addChannelData( 305, junkData, pos, i, j, k, md );
                }
            }
        }
    }

    private void createMaskAcceptor(FilteringAcceptorDecorator filter) throws Exception {
        for ( int i = 0; i < 300; i++ ) {
            for ( int j = 0; j < 300; j++ ) {
                for ( int k = 0; k < 300; k++ ) {
                    //(Integer orignalMaskNum, byte[] channelData, long position, long x, long y, long z, ChannelMetaData channelMetaData)
                    long pos = MockAcceptor.X_EXTENT * i + MockAcceptor.Y_EXTENT * j + MockAcceptor.Z_EXTENT * k;
                    filter.addMaskData(305, pos, i, j, k);
                }
            }
        }
    }

    private static long[] breakDownToCoords(Long nextPos) {
        long[] coords = new long[ 3 ];
        // Need to break this down into its constituent parts.
        // The 'X' part will be whatever is left after dividing by max X extent.
        long xPart = nextPos % MockAcceptor.X_EXTENT;
        coords[ 0 ] = xPart;

        long nonXPart = (nextPos - xPart) / MockAcceptor.X_EXTENT;
        long yPart = nonXPart % MockAcceptor.Y_EXTENT;
        coords[ 1 ] = yPart;

        long zPart = (nonXPart - yPart) / MockAcceptor.Y_EXTENT;
        coords[ 2 ] = zPart;
        return coords;
    }

}
