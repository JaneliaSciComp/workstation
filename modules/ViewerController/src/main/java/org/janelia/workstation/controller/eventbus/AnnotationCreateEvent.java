package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.workstation.controller.NeuronManager;

import java.util.Collection;
import java.util.List;

public class AnnotationCreateEvent extends AnnotationEvent {
    public AnnotationCreateEvent(Object source,
                                 Collection<TmGeoAnnotation> annotations, TmGeoAnnotation nextParent) {
        super(source);
        this.annotations = annotations;
        this.requestedNextParent = nextParent;
    }
}
