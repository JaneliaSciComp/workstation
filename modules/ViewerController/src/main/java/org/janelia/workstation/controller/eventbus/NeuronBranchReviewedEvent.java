package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import java.util.Collection;

public class NeuronBranchReviewedEvent extends WorkflowEvent {
    protected Collection<TmGeoAnnotation> annotations;
    public Collection<TmGeoAnnotation> getAnnotations() {
        return annotations;
    }
    public void setAnnotations(Collection<TmGeoAnnotation> annotations) {
        this.annotations = annotations;
    }
}