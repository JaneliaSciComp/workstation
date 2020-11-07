package org.janelia.workstation.controller.listener;

import org.janelia.workstation.controller.model.annotations.neuron.VertexCollectionWithNeuron;

/**
 *
 * @author brunsc
 */
public interface NeuronVertexDeletionListener {

    void neuronVertexesDeleted(VertexCollectionWithNeuron vertexesWithNeurons);
    
}
