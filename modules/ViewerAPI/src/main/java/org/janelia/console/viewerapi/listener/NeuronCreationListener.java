package org.janelia.console.viewerapi.listener;

import java.util.Collection;
import org.janelia.console.viewerapi.model.NeuronModel;

/**
 *
 * @author brunsc
 */
public interface NeuronCreationListener {

    void neuronsCreated(Collection<NeuronModel> createdNeurons);
    
}
