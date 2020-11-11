package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;

import java.util.Collection;

public class AnnotationParentReparentedEvent extends AnnotationEvent {
    private Long prevNeuronId;

    public AnnotationParentReparentedEvent(Collection<TmGeoAnnotation> annotations, Long prevNeuronId) {
        this.annotations = annotations;
        this.prevNeuronId = prevNeuronId;
    }

    public Long getPrevNeuronId() {
        return prevNeuronId;
    }
}
