package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import java.util.Collection;

abstract public class AnnotationEvent extends ViewerEvent {
    protected Collection<TmGeoAnnotation> annotations;
    protected TmGeoAnnotation requestedNextParent;

    public Collection<TmGeoAnnotation> getAnnotations() {
        return annotations;
    }

    public TmGeoAnnotation getRequestedNextParent() {
        return requestedNextParent;
    }

    public boolean hasRequestedNextParent() {
        return requestedNextParent != null;
    }
}
