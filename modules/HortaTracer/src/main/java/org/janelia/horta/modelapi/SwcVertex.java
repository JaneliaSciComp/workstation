package org.janelia.horta.modelapi;

import org.janelia.console.viewerapi.model.NeuronVertex;

/**
 * Full interface for SWC format neuron reconstructions
 *
 * @author Christopher Bruns
 */
public interface SwcVertex extends NeuronVertex {
    int getLabel();
    void setLabel(int label);
    int getTypeIndex();
    void setTypeIndex(int index);
    SwcVertex getParent();
    void setParent(SwcVertex parent);
}
