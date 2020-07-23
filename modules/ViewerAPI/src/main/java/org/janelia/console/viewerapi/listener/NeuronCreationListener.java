package org.janelia.console.viewerapi.listener;

import java.util.Collection;

import org.janelia.console.viewerapi.components.TmNeuronPanel;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;

/**
 *
 * @author brunsc
 */
public interface NeuronCreationListener {

    void neuronsCreated(Collection<TmNeuronMetadata> createdNeurons);
    
}
