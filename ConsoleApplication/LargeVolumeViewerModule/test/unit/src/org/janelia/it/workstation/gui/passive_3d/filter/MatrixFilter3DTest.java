/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.passive_3d.filter;

import java.nio.ByteOrder;
import org.janelia.it.jacs.model.TestCategories;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests the Matrix Filter 3D class.
 * @author fosterl
 */
public class MatrixFilter3DTest {
    /*
    NOTE:  The matrix cell values below, are all multiples of 27.
           The averaging function is 1/27, in the smoothing matrix.
           Therefore, once the calculations are complete, the
           centermost value will be identical to ALL values of the
           input matrix.
    */
    private static final byte[] TEST_INPUT_1B_3_3_3 = new byte[] {
        27, 27, 27,
        27, 27, 27,
        27, 27, 27,

        27, 27, 27,
        27, 27, 27,
        27, 27, 27,

        27, 27, 27,
        27, 27, 27,
        27, 27, 27,
    };

    private static final byte[] TEST_INPUT_2B_3_3_3 = new byte[] {
        0,81, 0,81, 0,81,
        0,81, 0,81, 0,81,
        0,81, 0,81, 0,81,

        0,81, 0,81, 0,81,
        0,81, 0,81, 0,81,
        0,81, 0,81, 0,81,

        0,81, 0,81, 0,81,
        0,81, 0,81, 0,81,
        0,81, 0,81, 0,81,
    };
    
    private static final byte[] TEST_INPUT_2BLARGE_3_3_3 = new byte[] {
        1,14, 1,14, 1,14,
        1,14, 1,14, 1,14,
        1,14, 1,14, 1,14,

        1,14, 1,14, 1,14,
        1,14, 1,14, 1,14,
        1,14, 1,14, 1,14,

        1,14, 1,14, 1,14,
        1,14, 1,14, 1,14,
        1,14, 1,14, 1,14,
    };
    
    @Test
    @Category(TestCategories.FastTests.class)
    public void filter1b() {
        int bytesPerVoxel = 1;
        MatrixFilter3D matrixFilter = new MatrixFilter3D( MatrixFilter3D.AVG_MATRIX_3_3_3, ByteOrder.BIG_ENDIAN );
        byte[] result = matrixFilter.filter(TEST_INPUT_1B_3_3_3, bytesPerVoxel, 1, 3, 3, 3);
        System.out.println(testDump( result, bytesPerVoxel ));
        assert result[ result.length / 2 ] == TEST_INPUT_1B_3_3_3[ 0 ] : "Failed to find expected center value.";
    }

    @Test
    @Category(TestCategories.FastTests.class)
    public void filter2b() {
        int bytesPerVoxel = 2;
        MatrixFilter3D matrixFilter = new MatrixFilter3D( MatrixFilter3D.AVG_MATRIX_3_3_3, ByteOrder.BIG_ENDIAN );
        byte[] result = matrixFilter.filter(TEST_INPUT_2B_3_3_3, bytesPerVoxel, 1, 3, 3, 3);
        System.out.println(testDump( result, bytesPerVoxel ));
        assert result[ result.length / 2 ] == TEST_INPUT_2B_3_3_3[ 1 ] : "Failed to find expected center value.";
    }
    
    @Test
    @Category(TestCategories.FastTests.class)
    public void filter2bLarge() {
        int bytesPerVoxel = 2;
        MatrixFilter3D matrixFilter = new MatrixFilter3D( MatrixFilter3D.AVG_MATRIX_3_3_3, ByteOrder.BIG_ENDIAN );
        byte[] result = matrixFilter.filter(TEST_INPUT_2BLARGE_3_3_3, bytesPerVoxel, 1, 3, 3, 3);
        System.out.println(testDump( result, bytesPerVoxel ));
        assert result[ result.length / 2 ] == TEST_INPUT_2BLARGE_3_3_3[ 1 ] : "Failed to find expected center value.";
    }
    
    @Test
    @Category(TestCategories.FastTests.class)
    public void filter1dSet() {
        int bytesPerVoxel = 2;
        MatrixFilter3D matrixFilter = new MatrixFilter3D( MatrixFilter3D.AVG_MATRIX_3_3_3, ByteOrder.BIG_ENDIAN );
        MatrixFilter3D.Dim1Matrices matrices = matrixFilter.matrix1dSelector(MatrixFilter3D.AVG_MATRIX_3_3_3, 3);
        assert matrices != null;
        matrixFilter = new MatrixFilter3D( MatrixFilter3D.GAUSS_65_85_85, ByteOrder.BIG_ENDIAN );
        matrices = matrixFilter.matrix1dSelector();
        double[] testResultMatrix = new double[] {
            0.02951906610780948, 0.23537051469301593, 0.4702208383983492, 0.23537051469301593, 0.02951906610780948 
        };
        double[] deepMatrix = matrices.getDeep();
        for ( int i = 0; i < deepMatrix.length; i++ ) {
            if (deepMatrix[i] != testResultMatrix[i]) {
                Assert.fail("Deep matrix from GAUSS 65,85,85 does not match.");
            }
        }
        assert matrices != null;
    }
    
    private String testDump( byte[] inputValue, int byteCount ) {
        StringBuilder bldr = new StringBuilder();
        for ( int i = 0; i < inputValue.length; i += byteCount ) {
            if ( i > 0 ) {
                bldr.append( ";" );
            }
            for ( int j = 0; j < byteCount; j++ ) {
                bldr.append( inputValue[ i + j ] );
                bldr.append( " " );
            }
        }
        return bldr.toString();
    }
}
