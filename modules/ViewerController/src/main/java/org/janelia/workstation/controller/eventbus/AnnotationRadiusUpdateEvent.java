package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;

import java.util.Collection;

public class AnnotationRadiusUpdateEvent extends AnnotationUpdateEvent {
    public AnnotationRadiusUpdateEvent(Object source,
                                       Collection<TmGeoAnnotation> annotations,
                                       TmGeoAnnotation nextParent) {
        super(source, annotations, nextParent);
    }
}

