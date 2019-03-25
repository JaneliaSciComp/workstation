/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.passive_3d.filter;

import org.janelia.it.workstation.gui.passive_3d.DimensionConvertor;
import org.junit.Test;

/**
 * Tests that matrix-build operation, to support filtering, produces expected
 * results.
 * @author fosterl
 */
public class GaussianMatrixBuilderTest {
    private static final String SIGMA_DUMP_FMT = "SigmaX=%06.3f  SigmaY=%06.3f  SigmaZ=%06.3f";
    @Test
    public void sigma3D() {
        GaussianMatrixBuilder bldr = new GaussianMatrixBuilder();
        double sigmaX = 0.65;
        double sigmaY = 0.85;
        double sigmaZ = 0.85;
        double[] filterMatrix = bldr.getGaussianMatrix(sigmaX, sigmaY, sigmaZ, 5);
        
        String matrixDump = dumpFilterMatrix( filterMatrix, 5, String.format( SIGMA_DUMP_FMT, sigmaX, sigmaY, sigmaZ) );
        System.out.println(matrixDump);
    }
    
    private String dumpFilterMatrix( double[] filterMatrix, int cubicDimension, String subdescription ) {
        double totalValue = 0.0;
        StringBuilder bldr = new StringBuilder("Filter Matrix of Dimension ").append(cubicDimension).append("\n");
        bldr.append( subdescription ).append("\n");
        DimensionConvertor dcon = new DimensionConvertor( cubicDimension );
        for ( int z = 0; z < cubicDimension; z++ ) {
            bldr.append("Sheet #").append(z+1).append("\n");
            for ( int y = 0; y < cubicDimension; y++ ) {
                for ( int x = 0; x < cubicDimension; x++ ) {
                    final double value = filterMatrix[ dcon.getLinearDimension(x, y, z) ];
                    bldr.append( value).append( '\t' );
                    totalValue += value;
                }
                bldr.append( "\n" );
            }
            bldr.append("\n");
        }
        bldr.append("Grand total of distribution is ").append( totalValue ).append('\n');
        return bldr.toString();
    }
}
