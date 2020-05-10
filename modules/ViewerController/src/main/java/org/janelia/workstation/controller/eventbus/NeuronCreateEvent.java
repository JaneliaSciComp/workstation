package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;

import java.util.Collection;

public class NeuronCreateEvent extends NeuronEvent {
    public NeuronCreateEvent(Collection<TmNeuronMetadata> neurons) {
        this.neurons = neurons;
    }
}
