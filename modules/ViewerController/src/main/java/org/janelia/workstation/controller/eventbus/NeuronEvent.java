package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import java.util.Collection;

abstract public class NeuronEvent {
    public Collection<TmNeuronMetadata> getNeurons() {
        return neurons;
    }

    public void setNeurons(Collection<TmNeuronMetadata> neurons) {
        this.neurons = neurons;
    }

    protected Collection<TmNeuronMetadata> neurons;
}
