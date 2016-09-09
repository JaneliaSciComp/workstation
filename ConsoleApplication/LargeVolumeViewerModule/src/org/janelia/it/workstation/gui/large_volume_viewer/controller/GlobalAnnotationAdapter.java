package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import org.janelia.it.jacs.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace;
import java.util.List;
import java.util.Map;

import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyle;

/**
 * 1 stubbed implementation of a listener, to avoid having to stub them in many
 * places.
 * 
 * @author fosterl
 */
public abstract class GlobalAnnotationAdapter implements GlobalAnnotationListener {

    @Override
    public void workspaceLoaded(TmWorkspace workspace) {}

    @Override
    public void neuronSelected(TmNeuronMetadata neuron) {}

    @Override
    public void neuronStyleChanged(TmNeuronMetadata neuron, NeuronStyle style) {}

    @Override
    public void neuronStylesChanged(Map<TmNeuronMetadata, NeuronStyle> neuronStyleMap) {}

    @Override
    public void neuronTagsChanged(List<TmNeuronMetadata> neuronList) {}
    
}
