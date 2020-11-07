package org.janelia.workstation.controller.listener;

import java.util.Collection;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;

/**
 *
 * @author brunsc
 */
public interface NeuronCreationListener {

    void neuronsCreated(Collection<TmNeuronMetadata> createdNeurons);
    
}
