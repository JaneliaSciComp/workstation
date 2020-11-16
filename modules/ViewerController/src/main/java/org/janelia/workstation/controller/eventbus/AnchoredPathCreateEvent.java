package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmAnchoredPath;

import java.util.Collection;

public class AnchoredPathCreateEvent extends AnchoredPathEvent {
    public AnchoredPathCreateEvent(Object source,
                                   Long neuronID,
                                   Collection<TmAnchoredPath> paths) {
        super(source);
        this.neuronID = neuronID;
        this.paths = paths;
    }
}
