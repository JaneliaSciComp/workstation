package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;

import java.util.Collection;

public class AnnotationNotesUpdateEvent extends NeuronEvent {
    protected Collection<TmGeoAnnotation> annotations;

    public Collection<TmGeoAnnotation> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Collection<TmGeoAnnotation> annotations) {
        this.annotations = annotations;
    }
}

