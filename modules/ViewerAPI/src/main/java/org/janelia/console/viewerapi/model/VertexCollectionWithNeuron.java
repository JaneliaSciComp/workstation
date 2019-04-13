package org.janelia.console.viewerapi.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 *
 * @author brunsc
 */
public class VertexCollectionWithNeuron {
    public Collection<NeuronVertex> vertexes;
    public NeuronModel neuron;
    
    public VertexCollectionWithNeuron(Collection<NeuronVertex> vertexes, NeuronModel neuronModel) {
        this.vertexes = vertexes;
        this.neuron = neuronModel;
    }
    
    public VertexCollectionWithNeuron(NeuronVertex vertex, NeuronModel neuronModel) {
        this.vertexes = new ArrayList<>(Arrays.asList(vertex));
        this.neuron = neuronModel;
    }
}
