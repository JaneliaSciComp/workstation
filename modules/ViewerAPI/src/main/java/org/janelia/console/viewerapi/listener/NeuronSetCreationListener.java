package org.janelia.console.viewerapi.listener;

import java.util.Collection;
import org.janelia.console.viewerapi.model.NeuronSet;

// Compact API for listening for new neuron models added to the workspace

// "NeuronSet"s are like LVV workspaces.
public interface NeuronSetCreationListener {

    void neuronSetsCreated(Collection<NeuronSet> createdNeurons);
    
}
