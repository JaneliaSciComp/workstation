/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.passive_3d;

/**
 * Converts between linear and triple dimensioned matrix positions.
 * 
 * @author fosterl
 */
public class DimensionConvertor {
    private int xDim, yDim, zDim, stride;
    private int sheetSize;
    private int lineSize;
    
    /**
     * Simple case constructor.  Stride is only one, and same dimensions for 
     * all axes.
     * 
     * @param cubicDim how many cells along _all_axes?
     */
    public DimensionConvertor( int cubicDim ) {
        this( cubicDim, cubicDim, cubicDim, 1 );
    }
    
    /**
     * Construct will all needed to convert.
     * 
     * @param xDim how many cells along x axis?
     * @param yDim ...along y axis?
     * @param zDim ...along z axis?
     * @param stride how wide is each cell; often only one.
     */
    public DimensionConvertor( int xDim, int yDim, int zDim, int stride ) {
        this.xDim = xDim;
        this.yDim = yDim;
        this.zDim = zDim;
        this.stride = stride;
        this.lineSize = (stride * xDim);
        this.sheetSize = (lineSize * yDim);
    }
    
    public int getLinearDimension( int x, int y, int z ) {
        return z * sheetSize + y * lineSize + x * stride;
    }
}
