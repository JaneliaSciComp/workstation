package org.janelia.console.viewerapi.listener;

import org.janelia.console.viewerapi.model.VertexWithNeuron;

// Signals that one vertex was added manually. Not to be used for bulk loading.

public interface NeuronVertexCreationListener {

    void neuronVertexCreated(VertexWithNeuron vertexWithNeuron);
    
}
