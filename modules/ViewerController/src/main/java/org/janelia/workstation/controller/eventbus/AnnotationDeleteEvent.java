package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import java.util.Collection;

public class AnnotationDeleteEvent extends AnnotationEvent {
    private Collection<TmGeoAnnotation> annotations;

    public AnnotationDeleteEvent(Collection<TmGeoAnnotation> annotations) {
        this.annotations = annotations;
    }
}

