package org.janelia.console.viewerapi.listener;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;

import java.util.Collection;

/**
 *
 * @author brunsc
 */
public interface NeuronSelectionListener {
    void vertexSelected(TmGeoAnnotation selectedVertex);
    void neuronSelected(TmNeuronMetadata selectedNeuron);

}
