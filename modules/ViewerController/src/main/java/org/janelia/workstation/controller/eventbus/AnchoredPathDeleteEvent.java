package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmAnchoredPath;

import java.util.Collection;

public class AnchoredPathDeleteEvent extends AnchoredPathEvent {
    public AnchoredPathDeleteEvent(Collection<TmAnchoredPath> paths) {
        this.paths = paths;
    }
}
