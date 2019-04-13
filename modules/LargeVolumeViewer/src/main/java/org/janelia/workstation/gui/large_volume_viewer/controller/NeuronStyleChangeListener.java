package org.janelia.workstation.gui.large_volume_viewer.controller;

import java.util.Map;

import org.janelia.workstation.gui.large_volume_viewer.style.NeuronStyle;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;

/**
 * implement to hear when neurons change styles
 *
 * djo, 3/15
 */
public interface NeuronStyleChangeListener {
    void neuronStyleChanged(TmNeuronMetadata neuron, NeuronStyle style);
    void neuronStylesChanged(Map<TmNeuronMetadata, NeuronStyle> neuronStyleMap);
    void neuronStyleRemoved(TmNeuronMetadata neuron);
}
