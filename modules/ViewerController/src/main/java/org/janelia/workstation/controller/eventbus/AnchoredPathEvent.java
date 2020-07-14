package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmAnchoredPath;
import java.util.Collection;

abstract public class AnchoredPathEvent extends ViewerEvent {
    protected Collection<TmAnchoredPath> paths;
    protected Long neuronID;

    public Long getNeuronID() {
        return neuronID;
    }
    public void setNeuronID(Long neuronID) {
        this.neuronID = neuronID;
    }

    public Collection<TmAnchoredPath> getPaths() {
        return paths;
    }
    public void setAnnotations(Collection<TmAnchoredPath> paths) {
        this.paths = paths;
    }

}
