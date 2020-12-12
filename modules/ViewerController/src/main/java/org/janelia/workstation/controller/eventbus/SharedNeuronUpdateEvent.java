package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;

import java.util.Collection;

public class SharedNeuronUpdateEvent extends NeuronEvent {
    public SharedNeuronUpdateEvent(Object source,
                                   Collection<TmNeuronMetadata> neurons) {
        super(source, neurons);
    }
}

