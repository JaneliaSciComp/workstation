package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;

import java.util.Collection;

public class NeuronTagsUpdateEvent extends NeuronEvent {
    protected Collection<TmNeuronMetadata> neurons;

    public NeuronTagsUpdateEvent(Object source,
                                 Collection<TmNeuronMetadata> neurons) {
        super(source, neurons);
    }

    public Collection<TmNeuronMetadata> getNeurons() {
        return neurons;
    }
}

