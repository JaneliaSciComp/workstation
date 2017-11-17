package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;

/**
 * Implement this to hear about a neuron having been clicked.
 * 
 * @author fosterl
 */
public interface NeuronSelectedListener {
    void selectNeuron(TmNeuronMetadata neuron);
}
