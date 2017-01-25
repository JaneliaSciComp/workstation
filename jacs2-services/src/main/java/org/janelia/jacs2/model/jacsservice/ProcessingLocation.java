package org.janelia.jacs2.model.jacsservice;

import org.janelia.jacs2.asyncservice.qualifier.LocalJob;
import org.janelia.jacs2.asyncservice.qualifier.ClusterJob;

import java.lang.annotation.Annotation;

public enum ProcessingLocation {
    LOCAL(LocalJob.class),
    CLUSTER(ClusterJob.class);

    private final Class<? extends Annotation> processingAnnotationClass;

    ProcessingLocation(Class<? extends Annotation> processingAnnotationClass) {
        this.processingAnnotationClass = processingAnnotationClass;
    }

    public Class<? extends Annotation> getProcessingAnnotationClass() {
        return processingAnnotationClass;
    }

}
