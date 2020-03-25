package org.janelia.workstation.controller.model;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;

import java.util.List;

public class TmSelectionState {
    private TmWorkspace currWorkspace;
    private TmSample currSample;
    private TmNeuronMetadata currNeuron;
    private List<TmGeoAnnotation> currPoints;

    public TmWorkspace getCurrWorkspace() {
        return currWorkspace;
    }

    public void setCurrWorkspace(TmWorkspace currWorkspace) {
        this.currWorkspace = currWorkspace;
    }

    public TmSample getCurrSample() {
        return currSample;
    }

    public void setCurrSample(TmSample currSample) {
        this.currSample = currSample;
    }

    public TmNeuronMetadata getCurrNeuron() {
        return currNeuron;
    }

    public void setCurrNeuron(TmNeuronMetadata currNeuron) {
        this.currNeuron = currNeuron;
    }

    public List<TmGeoAnnotation> getCurrPoints() {
        return currPoints;
    }

    public void setCurrPoints(List<TmGeoAnnotation> currPoints) {
        this.currPoints = currPoints;
    }
}
