package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;

import java.util.Collection;

public class AnnotationRadiusUpdateEvent extends AnnotationUpdateEvent {
    public AnnotationRadiusUpdateEvent(Collection<TmGeoAnnotation> annotations, TmGeoAnnotation nextParent) {
        super(annotations, nextParent);
    }
}

