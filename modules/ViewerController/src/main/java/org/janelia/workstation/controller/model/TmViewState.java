package org.janelia.workstation.controller.model;

import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.workstation.controller.spatialfilter.NeuronSelectionSpatialFilter;

public class TmViewState {
    private boolean applyFilter;
    private Vec3 cameraLocation;
    private NeuronSelectionSpatialFilter neuronFilter;

    public TmViewState() {
        setNeuronFilter(new NeuronSelectionSpatialFilter());
    }

    public boolean isApplyFilter() {
        return applyFilter;
    }

    public void setApplyFilter(boolean applyFilter) {
        this.applyFilter = applyFilter;
    }

    public NeuronSelectionSpatialFilter getNeuronFilter() {
        return neuronFilter;
    }

    public void setNeuronFilter(NeuronSelectionSpatialFilter neuronFilter) {
        this.neuronFilter = neuronFilter;
    }
}
