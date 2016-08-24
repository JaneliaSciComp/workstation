package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import java.util.Map;

import org.janelia.it.jacs.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyle;

/**
 * implement to hear when neurons change styles
 *
 * djo, 3/15
 */
public interface NeuronStyleChangeListener {
    void neuronStyleChanged(TmNeuronMetadata neuron, NeuronStyle style);
    void neuronStylesChanged(Map<TmNeuronMetadata, NeuronStyle> neuronStyleMap);
}
