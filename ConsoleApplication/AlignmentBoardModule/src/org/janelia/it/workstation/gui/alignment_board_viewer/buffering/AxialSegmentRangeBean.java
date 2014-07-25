/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.alignment_board_viewer.buffering;

/**
 * Bag-o-data for range limits defining the segments of axes.  Makes sub-
 * ranges in all three dimensions.
 * 
 * @author fosterl
 */
public class AxialSegmentRangeBean {
    public static final int LOW_INX = 0;
    public static final int HIGH_INX = 1;        
    
    private int[] xRange;
    private int[] yRange;
    private int[] zRange;

    private boolean partialx;
    private boolean partialy;
    private boolean partialz = true; //TEMP

    /**
     * Use canonical axial order 0,1,2 for x,y,z, and return whichever
     * range corresponds to the axis # given.
     * 
     * @param axialOffset 0,1,2 => x,y,z
     * @return corresponding start/end range.
     */
    public int[] getRangeByAxisNum( int axialOffset ) {
        switch( axialOffset ) {
            case 0 : return xRange;
            case 1 : return yRange;
            case 2 : return zRange;
            default: return null;
        }
    }

    @SuppressWarnings("unused")
    public boolean isPartialByAxisNum( int axialOffset ) {
        switch( axialOffset ) {
            case 0: return partialx;
            case 1: return partialy;
            case 2: return partialz;
            default: return false;
        }
    }
    
    public int getAxisLength( int axialOffset ) {
        final int[] rangeArr = getRangeByAxisNum( axialOffset );
        return rangeArr[ HIGH_INX ] - rangeArr[ LOW_INX ];
    }
    
    public int getAxisLow( int axialOffset ) {
        return getRangeByAxisNum( axialOffset )[ LOW_INX ];
    }
    
    public int getAxisHigh( int axialOffset ) {
        return getRangeByAxisNum( axialOffset )[ HIGH_INX ];
    }
    
    /**
     * @return the xRange
     */
    @SuppressWarnings("unused")
    public int[] getxRange() {
        return xRange;
    }

    /**
     * @param xRange the xRange to set
     */
    public void setxRange(int[] xRange) {
        testRange(xRange);
        this.xRange = xRange;
    }

    /**
     * @return the yRange
     */
    @SuppressWarnings("unused")
    public int[] getyRange() {
        return yRange;
    }

    /**
     * @param yRange the yRange to set
     */
    public void setyRange(int[] yRange) {
        testRange(yRange);
        this.yRange = yRange;
    }

    /**
     * @return the zRange
     */
    @SuppressWarnings("unused")
    public int[] getzRange() {
        return zRange;
    }

    /**
     * @param zRange the zRange to set
     */
    public void setzRange(int[] zRange) {
        testRange(zRange);
        this.zRange = zRange;
    }
                
    public boolean isPartialx() {
        return partialx;
    }

    @SuppressWarnings("unused")
    public void setPartialx(boolean partialx) {
        this.partialx = partialx;
    }

    public boolean isPartialy() {
        return partialy;
    }

    @SuppressWarnings("unused")
    public void setPartialy(boolean partialy) {
        this.partialy = partialy;
    }

    public boolean isPartialz() {
        return partialz;
    }

    @SuppressWarnings("unused")
    public void setPartialz(boolean partialz) {
        this.partialz = partialz;
    }

    private void testRange(int[] range) throws IllegalArgumentException {
        if ( range.length != 2 ) {
            throw new IllegalArgumentException("Invalid range array size.   Must be exactly 2.");
        }
        if ( range[ LOW_INX ] > range[ HIGH_INX ] ) {
            throw new IllegalArgumentException("Invalid range: end < start");
        }
    }

}
