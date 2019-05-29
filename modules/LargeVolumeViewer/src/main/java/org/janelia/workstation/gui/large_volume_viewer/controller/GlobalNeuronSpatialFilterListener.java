package org.janelia.workstation.gui.large_volume_viewer.controller;

import org.janelia.workstation.gui.large_volume_viewer.annotation.NeuronSpatialFilter;

public interface GlobalNeuronSpatialFilterListener {

    void neuronSpatialFilterUpdated(boolean enabled, NeuronSpatialFilter filter);
}
