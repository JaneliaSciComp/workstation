package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_export;

import org.janelia.it.FlyWorkstation.gui.alignment_board.loader.ChannelMetaData;
import org.janelia.it.FlyWorkstation.gui.alignment_board.loader.MaskChanDataAcceptorI;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * This simple acceptor just keeps track of everything sent to it.  There is no distinction between mask and channel
 * data.
 * Created by fosterl on 1/29/14.
 */
public class MockAcceptor implements MaskChanDataAcceptorI {
    public static int X_EXTENT = 1024;
    public static int Y_EXTENT = 512;
    public static int Z_EXTENT = 128;

    private List<Long> occupiedPositions = new ArrayList<Long>();

    @Override
    public int addChannelData(Integer orignalMaskNum, byte[] channelData, long position, long x, long y, long z, ChannelMetaData channelMetaData) throws Exception {
        checkCoordConsistency(position, x, y, z);
        occupiedPositions.add( position );
        return 1;
    }

    @Override
    public int addMaskData(Integer maskNumber, long position, long x, long y, long z) throws Exception {
        checkCoordConsistency(position, x, y, z);
        occupiedPositions.add(position);
        return 1;
    }

    @Override
    public void setSpaceSize(long x, long y, long z, long paddedX, long paddedY, long paddedZ, float[] coordCoverage) {

    }

    @Override
    public Acceptable getAcceptableInputs() {
        return Acceptable.both;
    }

    @Override
    public int getChannelCount() {
        return 3;
    }

    @Override
    public void setChannelMetaData(ChannelMetaData metaData) {

    }

    @Override
    public void endData(Logger logger) {

    }

    public List<Long> getOccupiedPositions() {
        return occupiedPositions;
    }

    private void checkCoordConsistency(long position, long x, long y, long z) {
        long calPos = calculateLinearOffset(x, y, z);
        if (  calPos !=  position ) {
            throw new IllegalArgumentException("Test data inconsistent: position must reflect its 1-byte-slot x,y,z coords");
        }
    }

    public static long calculateLinearOffset(long x, long y, long z) {
        long sheetOffset = (Y_EXTENT * X_EXTENT * z );
        long lineOffset = (X_EXTENT * y);
        long charOffset = x;
        return sheetOffset + lineOffset + charOffset;
    }

    public static ChannelMetaData getTestableMetaData() {
        ChannelMetaData cmd = new ChannelMetaData();
        cmd.blueChannelInx = 2;
        cmd.redChannelInx = 0;
        cmd.greenChannelInx = 1;
        cmd.byteCount = 1;
        cmd.channelCount = 4;
        cmd.rawChannelCount = 3;
        return cmd;
    }

}
