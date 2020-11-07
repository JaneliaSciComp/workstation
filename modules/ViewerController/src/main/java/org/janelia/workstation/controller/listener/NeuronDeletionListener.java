package org.janelia.workstation.controller.listener;

import java.util.Collection;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;

/**
 *
 * @author brunsc
 */
public interface NeuronDeletionListener {

    void neuronsDeleted(Collection<TmNeuronMetadata> deletedNeurons);
    
}
