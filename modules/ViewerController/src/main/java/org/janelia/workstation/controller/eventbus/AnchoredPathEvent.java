package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmAnchoredPath;
import java.util.Collection;

abstract public class AnchoredPathEvent extends ViewerEvent {
    protected Collection<TmAnchoredPath> paths;
    protected Long neuronID;

    public AnchoredPathEvent(Object sourceClass) {
        super(sourceClass);
    }

    public Long getNeuronID() {
        return neuronID;
    }

    public Collection<TmAnchoredPath> getPaths() {
        return paths;
    }

}
