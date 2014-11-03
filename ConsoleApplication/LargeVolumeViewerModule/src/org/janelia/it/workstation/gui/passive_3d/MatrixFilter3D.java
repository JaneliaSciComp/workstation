/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.passive_3d;

/**
 * Carries out a 3D filtering (for things like smoothing), against
 * some input byte array, which has N bytes per element.
 * 
 * @author fosterl
 */
public class MatrixFilter3D {
    private static final double AVG_VAL = 1.0/27.0; 
    public static double[] AVG_MATRIX = new double[] {
        AVG_VAL, AVG_VAL, AVG_VAL,
        AVG_VAL, AVG_VAL, AVG_VAL,
        AVG_VAL, AVG_VAL, AVG_VAL,

        AVG_VAL, AVG_VAL, AVG_VAL,
        AVG_VAL, AVG_VAL, AVG_VAL,
        AVG_VAL, AVG_VAL, AVG_VAL,

        AVG_VAL, AVG_VAL, AVG_VAL,
        AVG_VAL, AVG_VAL, AVG_VAL,
        AVG_VAL, AVG_VAL, AVG_VAL,
    };
    
    private double[] matrix;
    private int matrixSquareDim;
    
    public MatrixFilter3D( double[] matrix ) {
        this.matrix = matrix;
        matrixSquareDim = (int)Math.sqrt( matrix.length );
        if ( matrixSquareDim * matrixSquareDim != matrix.length ) {
            throw new IllegalArgumentException( "Matrix size not a square." );
        }
    }
    
    /**
     * Filter the input array using the supplied matrix.
     * 
     * @param inputBytes bytes of input data
     * @param bytesPerCell how many bytes make up the integer cell value (1..4)
     * @param sx length of x.
     * @param sy length of y.
     * @param sz length of z.
     * @return filtered version of original.
     */
    public byte[] filter( byte[] inputBytes, int bytesPerCell, int sx, int sy, int sz ) {
        return inputBytes;
    }
}
