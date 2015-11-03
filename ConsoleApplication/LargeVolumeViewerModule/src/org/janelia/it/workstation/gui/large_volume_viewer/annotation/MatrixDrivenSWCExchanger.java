/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import Jama.Matrix;
import org.janelia.it.workstation.gui.large_volume_viewer.TileFormat;
import org.janelia.it.jacs.shared.swc.ImportExportSWCExchanger;

/**
 * Uses matrices (based on JAMA package), to convert between internal and
 * external SWC coordinate systems.
 * 
 * @author fosterl
 */
public class MatrixDrivenSWCExchanger implements ImportExportSWCExchanger {
    public static final int EXPECTED_ARRAY_SIZE = 3;
    private TileFormat tileFormat;
    
    public MatrixDrivenSWCExchanger( TileFormat tileFormat ) {
        this.tileFormat = tileFormat;
    }

    @Override
    public double[] getInternal(double[] external) {
        Matrix externalMatrix = inputMatrix(external);
        Matrix internalMatrix = tileFormat.getMicronToVoxMatrix().times(externalMatrix);
        return outputMatrix(internalMatrix);
    }

    @Override
    public double[] getExternal(double[] internal) {
        Matrix internalMatrix = inputMatrix(internal);
        Matrix externalMatrix = tileFormat.getVoxToMicronMatrix().times(internalMatrix);
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
