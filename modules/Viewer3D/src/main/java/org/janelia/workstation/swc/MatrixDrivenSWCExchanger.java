package org.janelia.workstation.swc;

import Jama.Matrix;

/**
 * Uses matrices (based on JAMA package), to convert between internal and
 * external SWC coordinate systems.
 * 
 * @author fosterl
 */
public class MatrixDrivenSWCExchanger implements ImportExportSWCExchanger {
    public static final int EXPECTED_ARRAY_SIZE = 3;
    private Matrix micronToVoxMatrix;
    private Matrix voxToMicronMatrix;
    
    public MatrixDrivenSWCExchanger( Matrix micronToVoxMatrix, Matrix voxToMicronMatrix ) {
        this.micronToVoxMatrix = micronToVoxMatrix;
        this.voxToMicronMatrix = voxToMicronMatrix;
    }

    @Override
    public double[] getInternal(double[] external) {
        Matrix externalMatrix = inputMatrix(external);
        Matrix internalMatrix = micronToVoxMatrix.times(externalMatrix);
        return outputMatrix(internalMatrix);
    }

    @Override
    public double[] getExternal(double[] internal) {
        Matrix internalMatrix = inputMatrix(internal);
        Matrix externalMatrix = voxToMicronMatrix.times(internalMatrix);
        return outputMatrix(externalMatrix);
    }
    
    private Matrix inputMatrix( double[] input ) {
        if (input.length != 3) {
            throw new IllegalArgumentException("Very specific matrix requirements.");
        }
        Matrix matrix = new Matrix( EXPECTED_ARRAY_SIZE + 1, 1 );
        for (int i = 0; i < input.length; i++) {
            matrix.set(i, 0, input[i]);
        }
        matrix.set(EXPECTED_ARRAY_SIZE, 0, 1.0);
        return matrix;
    }
    
    private double[] outputMatrix( Matrix output ) {
        double[] result = new double[EXPECTED_ARRAY_SIZE];
        for (int i = 0; i < result.length; i++ ) {
            result[i] = output.get(i, 0);
        }
        return result;
    }
    
}
