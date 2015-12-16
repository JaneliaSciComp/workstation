package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyle;

import java.util.Map;

/**
 * implement to hear when neurons change styles
 *
 * djo, 3/15
 */
public interface NeuronStyleChangeListener {
    void neuronStyleChanged(TmNeuron neuron, NeuronStyle style);
    void neuronStylesChanged(Map<TmNeuron, NeuronStyle> neuronStyleMap);
}
