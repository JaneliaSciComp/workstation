package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;

import java.util.ArrayList;
import java.util.Collection;

public class NeuronEvent extends ViewerEvent {
    public NeuronEvent(Object source,
                       Collection<TmNeuronMetadata> neurons) {
        super(source);
        if (neurons != null) {
            this.neurons = neurons;
        } else {
            this.neurons = new ArrayList<>();
        }
    }
    public Collection<TmNeuronMetadata> getNeurons() {
        return neurons;
    }

    protected Collection<TmNeuronMetadata> neurons;
}
