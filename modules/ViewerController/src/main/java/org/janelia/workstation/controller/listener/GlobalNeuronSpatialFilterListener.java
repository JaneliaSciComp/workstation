package org.janelia.workstation.controller.listener;
import org.janelia.workstation.controller.scripts.spatialfilter.NeuronSpatialFilter;

public interface GlobalNeuronSpatialFilterListener {

    void neuronSpatialFilterUpdated(boolean enabled, NeuronSpatialFilter filter);
}
