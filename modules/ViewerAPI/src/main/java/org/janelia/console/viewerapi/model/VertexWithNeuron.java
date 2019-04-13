package org.janelia.console.viewerapi.model;

/**
 *
 * @author brunsc
 */
public class VertexWithNeuron {
    public NeuronVertex vertex;
    public NeuronModel neuron;
    public VertexWithNeuron(NeuronVertex vertex, NeuronModel neuronModel) {
        this.vertex = vertex;
        this.neuron = neuronModel;
    }
}
