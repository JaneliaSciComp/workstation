package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;

import java.util.Collection;

public class NeuronOwnerChangedEvent extends NeuronUpdateEvent {

    public NeuronOwnerChangedEvent(Collection<TmNeuronMetadata> neurons) {
        super(neurons);
    }
}

