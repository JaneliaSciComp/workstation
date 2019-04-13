package org.janelia.workstation.gui.large_volume_viewer.neuron_api;

import org.janelia.console.viewerapi.model.NeuronVertex;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;

/**
 * Constraining filter for spatial queries. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface SpatialFilter {

    boolean include(NeuronVertex vertex, TmGeoAnnotation annotation);
}
