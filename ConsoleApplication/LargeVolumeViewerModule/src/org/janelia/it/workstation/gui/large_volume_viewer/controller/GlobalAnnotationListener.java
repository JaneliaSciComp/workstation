package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import org.janelia.it.jacs.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace;
import java.util.List;
import java.util.Map;

import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyle;

/**
 * Implement this to hear about workspace/all-annotation-scoped changes.
 * 
 * @author fosterl
 */
public interface GlobalAnnotationListener {
    void workspaceUnloaded(TmWorkspace workspace);
    void workspaceLoaded(TmWorkspace workspace);
    void spatialIndexReady(TmWorkspace workspace);
    void neuronCreated(TmNeuronMetadata neuron);
    void neuronDeleted(TmNeuronMetadata neuron);
    void neuronChanged(TmNeuronMetadata neuron);
    void neuronRenamed(TmNeuronMetadata neuron);    
    void neuronSelected(TmNeuronMetadata neuron);
    void neuronRadiusUpdated(TmNeuronMetadata neuron);
    void neuronStyleChanged(TmNeuronMetadata neuron, NeuronStyle style);
    void neuronStylesChanged(Map<TmNeuronMetadata, NeuronStyle> neuronStyleMap);
    void neuronTagsChanged(List<TmNeuronMetadata> neuronList);
    
}
