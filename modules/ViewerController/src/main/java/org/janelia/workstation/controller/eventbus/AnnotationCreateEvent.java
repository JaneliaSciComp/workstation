package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import java.util.Collection;

public class AnnotationCreateEvent extends AnnotationEvent {
        private Collection<TmGeoAnnotation> annotations;

        public AnnotationCreateEvent(Collection<TmGeoAnnotation> annotations) {
            this.annotations = annotations;
        }
}
