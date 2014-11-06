/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.passive_3d.filter;

import org.janelia.it.workstation.gui.passive_3d.DimensionConvertor;

/**
 * Use this to create a 3D/linear-represented gaussian matrix.
 * @author fosterl
 */
public class GaussianMatrixBuilder {
    /**
     * Produce a GAUSS filter matrix.  Use the sigma values for each respective
     * dimension (x, y, z), as given.  Use the supplied cubic dimension for the
     * matrix size (same in each dimension; for example 3x3x3, would take a
     * cubicDimension value of 3).  This is to be the size of the resulting
     * matrix: any given cell may be set to zero.
     * 
     * @param sigmaX parameter to the classic sigma (bell curve) function in X
     * @param sigmaY in Y
     * @param sigmaZ in Z
     * @param cubicDimension size of resulting matrix.
     * @return matrix suitable for filtering a like-sized neighborhood of point.
     */
    public double[] getGaussianMatrix( double sigmaX, double sigmaY, double sigmaZ, int cubicDimension ) {
        double[] rtnVal = new double[ cubicDimension * cubicDimension * cubicDimension ];
        final double sigmaFactor = 2.0 * sigmaX * sigmaY * sigmaZ; 
        double normalizer = 1.0 / (sigmaX * sigmaY * sigmaZ * Math.pow(2.0 * Math.PI, 3.0/2.0));
        // DEBUG: normalizer = 1.0; With this in place, center-value is 1.0
        int dimSubtractor = cubicDimension / 2;  // Center all values around 0-as-midpoint.
        DimensionConvertor dcon = new DimensionConvertor( cubicDimension );
        for ( int z = 0; z < cubicDimension; z++ ) {
            for ( int y = 0; y < cubicDimension; y++ ) {
                for ( int x = 0; x < cubicDimension; x++ ) {
                    rtnVal[ dcon.getLinearDimension(x, y, z) ] = 
                            normalizer * Math.exp(
                                -(
                                    (x-dimSubtractor)*(x-dimSubtractor) +
                                    (y-dimSubtractor)*(y-dimSubtractor) +
                                    (z-dimSubtractor)*(z-dimSubtractor)
                                 ) / sigmaFactor 
                     ); 
                }
            }
        }
        return rtnVal;
    }
}
