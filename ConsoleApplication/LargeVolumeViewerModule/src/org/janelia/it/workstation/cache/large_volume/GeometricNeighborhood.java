/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.cache.large_volume;

import java.io.File;
import java.util.Map;
import java.util.Set;
import org.janelia.it.workstation.gui.large_volume_viewer.components.model.PositionalStatusModel;

public interface GeometricNeighborhood {

	Set<File> getFiles();
    Double getZoom();
    double[] getFocus();
    int getId();
    Map<String,PositionalStatusModel> getPositionalModels();
}
