package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;

import java.util.Collection;

public class NeuronUnhideEvent extends NeuronEvent {
    public NeuronUnhideEvent(Object source,
                             Collection<TmNeuronMetadata> neurons) {
        super(source, neurons);
    }
}

