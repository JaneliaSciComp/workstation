package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;

import java.util.Collection;

public class NeuronTagsUpdateEvent extends NeuronEvent {
    protected Collection<TmNeuronMetadata> neurons;

    public Collection<TmNeuronMetadata> getNeurons() {
        return neurons;
    }

    public void setAnnotations(Collection<TmNeuronMetadata> annotations) {
        this.neurons = neurons;
    }
}

