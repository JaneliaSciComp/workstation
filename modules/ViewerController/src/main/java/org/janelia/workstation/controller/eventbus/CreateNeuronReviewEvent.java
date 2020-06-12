package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;

public class CreateNeuronReviewEvent extends WorkflowEvent {
    public TmNeuronMetadata getNeuron() {
        return neuron;
    }

    public void setNeuron(TmNeuronMetadata neuron) {
        this.neuron = neuron;
    }

    protected TmNeuronMetadata neuron;
}
