/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.shared.util;

/**
 * For use of importing and exporting SWC data.  Provides methods for making
 * the exchange between required SWC format and internal Neuron use.
 * 
 * At time of writing: internally, we use voxels, and externally, we
 * use micrometers. If that changes in future, the impl can reflect that,
 * without affecting this interface.  Likewise, double values are exchanged
 * both ways, regardless of whether integers/longs are actually used.
 * 
 * @author fosterl
 */
public interface ImportExportSWCExchanger {
    double[] getInternal( double[] external );
    double[] getExternal( double[] internal );
}
