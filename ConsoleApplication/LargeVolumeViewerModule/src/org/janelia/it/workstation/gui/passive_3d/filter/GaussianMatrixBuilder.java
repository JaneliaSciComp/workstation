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
     * @deprecated this has never been used. Begun for FW-2812. Using hard-coded matrixes instead.
     * @param sigmaX parameter to the classic sigma (bell curve) function in X
     * @param sigmaY in Y
     * @param sigmaZ in Z
     * @param cubicDimension size of resulting matrix.
     * @return matrix suitable for filtering a like-sized neighborhood of point.
     */
    public double[] getGaussianMatrix( double sigmaX, double sigmaY, double sigmaZ, int cubicDimension ) {
        double[] rtnVal = new double[ cubicDimension * cubicDimension * cubicDimension ];
        final double sigmaFactorX = 2.0 * sigmaX * sigmaX; 
        final double sigmaFactorY = 2.0 * sigmaY * sigmaY; 
        final double sigmaFactorZ = 2.0 * sigmaZ * sigmaZ; 
        // DEBUG: normalizer = 1.0; With this in place, center-value is 1.0
        int dimSubtractor = cubicDimension / 2;  // Center all values around 0-as-midpoint.
        DimensionConvertor dcon = new DimensionConvertor( cubicDimension );
        double sum = 0.0;
        for ( int z = 0; z < cubicDimension; z++ ) {
            for ( int y = 0; y < cubicDimension; y++ ) {
                for ( int x = 0; x < cubicDimension; x++ ) {
                    double xPart = Math.exp(
                                -(
                                    (x-dimSubtractor)*(x-dimSubtractor)
                                  / sigmaFactorX ) );
                    
                    double yPart = Math.exp(
                                -(
                                    (y-dimSubtractor)*(y-dimSubtractor)
                                  / sigmaFactorY ) );
                    
                    double zPart = Math.exp(
                                -(
                                    (z-dimSubtractor)*(z-dimSubtractor)
                                  / sigmaFactorZ ) ); 
                    final double value = xPart + yPart + zPart;
                    
                    rtnVal[ dcon.getLinearDimension(x, y, z) ] = value;
                    sum += value;
                }
            }
        }
        // Normalize.
        for ( int i = 0; i < rtnVal.length; i++ ) {
            rtnVal[ i ] = rtnVal[ i ] / sum;
        }
        return rtnVal;
    }
}
