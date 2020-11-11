package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import java.util.Collection;

public class AnnotationCreateEvent extends AnnotationEvent {
    public AnnotationCreateEvent(Collection<TmGeoAnnotation> annotations, TmGeoAnnotation nextParent) {
        this.annotations = annotations;
        this.requestedNextParent = nextParent;
    }
}
