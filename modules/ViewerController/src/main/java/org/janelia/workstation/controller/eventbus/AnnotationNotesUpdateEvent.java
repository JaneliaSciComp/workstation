package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;

import java.util.Collection;

public class AnnotationNotesUpdateEvent extends ViewerEvent {
    protected Collection<TmGeoAnnotation> annotations;

    public AnnotationNotesUpdateEvent(Object source,
                                      Collection<TmGeoAnnotation> annotations) {
        super(source);
        this.annotations = annotations;
    }

    public Collection<TmGeoAnnotation> getAnnotations() {
        return annotations;
    }
}

