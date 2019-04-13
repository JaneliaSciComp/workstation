package org.janelia.console.viewerapi.listener;

import java.util.Collection;
import org.janelia.console.viewerapi.model.NeuronSet;

/**
 *
 * @author brunsc
 */
public interface NeuronSetDeletionListener {

    void neuronSetsDeleted(Collection<NeuronSet> deletedNeurons);
    
}
