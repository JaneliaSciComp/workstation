package org.janelia.console.viewerapi.model;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;

/**
 *
 * @author brunsc
 */
public class VertexWithNeuron {
    public TmGeoAnnotation vertex;
    public TmNeuronMetadata neuron;
    public VertexWithNeuron(TmGeoAnnotation vertex, TmNeuronMetadata neuronModel) {
        this.vertex = vertex;
        this.neuron = neuronModel;
    }
}
