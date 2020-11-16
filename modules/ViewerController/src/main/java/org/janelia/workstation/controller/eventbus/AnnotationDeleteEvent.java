package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import java.util.Collection;

public class AnnotationDeleteEvent extends AnnotationEvent {
    public AnnotationDeleteEvent(Object source,
                                 Collection<TmGeoAnnotation> annotations,
                                 TmGeoAnnotation nextParent) {
        super(source);
        this.annotations = annotations;
        this.requestedNextParent = nextParent;
    }
}

