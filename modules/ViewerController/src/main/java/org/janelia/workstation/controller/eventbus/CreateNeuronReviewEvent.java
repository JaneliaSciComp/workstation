package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;

public class CreateNeuronReviewEvent extends WorkflowEvent {
    public CreateNeuronReviewEvent(TmNeuronMetadata neuron) {
        this.neuron = neuron;
    }

    public TmNeuronMetadata getNeuron() {
        return neuron;
    }

    protected TmNeuronMetadata neuron;
}
