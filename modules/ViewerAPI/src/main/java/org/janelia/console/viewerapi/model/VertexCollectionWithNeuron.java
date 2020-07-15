package org.janelia.console.viewerapi.model;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 *
 * @author brunsc
 */
public class VertexCollectionWithNeuron {
    public Collection<TmGeoAnnotation> vertexes;
    public TmNeuronMetadata neuron;
    
    public VertexCollectionWithNeuron(Collection<TmGeoAnnotation> vertexes, TmNeuronMetadata neuronModel) {
        this.vertexes = vertexes;
        this.neuron = neuronModel;
    }
    
    public VertexCollectionWithNeuron(TmGeoAnnotation vertex, TmNeuronMetadata neuronModel) {
        this.vertexes = new ArrayList<>(Arrays.asList(vertex));
        this.neuron = neuronModel;
    }
}
