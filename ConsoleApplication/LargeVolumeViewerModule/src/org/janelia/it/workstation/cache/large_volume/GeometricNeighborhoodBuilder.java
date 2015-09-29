/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.cache.large_volume;

public interface GeometricNeighborhoodBuilder {

	GeometricNeighborhood buildNeighborhood(double[] focus, Double zoom, double pixelsPerSceneUnit);
    void setListener(GeometricNeighborhoodListener listener);

}
