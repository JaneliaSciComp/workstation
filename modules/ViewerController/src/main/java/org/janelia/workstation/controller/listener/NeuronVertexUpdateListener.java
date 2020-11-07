package org.janelia.workstation.controller.listener;

import org.janelia.workstation.controller.model.annotations.neuron.VertexWithNeuron;

// Signals that one vertex was added manually. Not to be used for bulk loading.

public interface NeuronVertexUpdateListener {

    void neuronVertexUpdated(VertexWithNeuron vertexWithNeuron);
    
}
