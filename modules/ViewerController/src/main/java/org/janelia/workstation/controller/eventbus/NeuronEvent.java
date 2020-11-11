package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import java.util.Collection;

public class NeuronEvent extends ViewerEvent {
    public NeuronEvent(Collection<TmNeuronMetadata> neurons) {
        this.neurons = neurons;
    }
    public Collection<TmNeuronMetadata> getNeurons() {
        return neurons;
    }

    protected Collection<TmNeuronMetadata> neurons;
}
