package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;

import java.util.Collection;

public class NeuronDeleteEvent extends NeuronEvent {
    private Collection<TmNeuronMetadata> neurons;

    public NeuronDeleteEvent(Collection<TmNeuronMetadata> neurons) {
        this.neurons = neurons;
    }
}

