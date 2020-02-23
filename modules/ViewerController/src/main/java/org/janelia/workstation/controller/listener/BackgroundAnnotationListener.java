package org.janelia.workstation.controller.listener;

import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;

/**
 * Handles background updates coming from refresh.  
 * 
 * @author schauderd
 */
public interface BackgroundAnnotationListener {
    void neuronModelChanged(TmNeuronMetadata neuron);
    void neuronModelCreated(TmNeuronMetadata neuron);
    void neuronModelDeleted(TmNeuronMetadata neuron);
    public void neuronOwnerChanged(TmNeuronMetadata neuron);
}

