package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;

import java.util.Collection;

public class NeuronHideEvent extends NeuronEvent {
    public NeuronHideEvent(Collection<TmNeuronMetadata> neurons) {
        super(neurons);
    }
}

