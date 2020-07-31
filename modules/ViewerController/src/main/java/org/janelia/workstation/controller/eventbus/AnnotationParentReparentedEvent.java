package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;

import java.util.Collection;

public class AnnotationParentReparentedEvent extends AnnotationEvent {
    private Long prevNeuronId;

    public AnnotationParentReparentedEvent(Collection<TmGeoAnnotation> annotations) {
        this.annotations = annotations;
    }

    public Long getPrevNeuronId() {
        return prevNeuronId;
    }

    public void setPrevNeuronId(Long parentId) {
        this.prevNeuronId = parentId;
    }
}
