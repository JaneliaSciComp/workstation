package org.janelia.console.viewerapi.listener;

import java.util.Collection;
import org.janelia.console.viewerapi.model.NeuronModel;

/**
 *
 * @author brunsc
 */
public interface NeuronDeletionListener {

    void neuronsDeleted(Collection<NeuronModel> deletedNeurons);
    
}
