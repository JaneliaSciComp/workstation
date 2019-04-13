package org.janelia.console.viewerapi.listener;

import org.janelia.console.viewerapi.model.VertexCollectionWithNeuron;

/**
 *
 * @author brunsc
 */
public interface NeuronVertexDeletionListener {

    void neuronVertexesDeleted(VertexCollectionWithNeuron vertexesWithNeurons);
    
}
