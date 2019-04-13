package org.janelia.workstation.gui.large_volume_viewer.controller;

import java.util.List;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;

/**
 * Implement this to be added as a listener for when TmGeoAnnotations are
 * modified.
 * 
 * @author fosterl
 */
public interface TmGeoAnnotationModListener {
    void annotationAdded(TmGeoAnnotation annotation);
    void annotationsDeleted(List<TmGeoAnnotation> annotations);
    void annotationReparented(TmGeoAnnotation annotation, Long prevNeuronId);
    void annotationNotMoved(TmGeoAnnotation annotation);
    void annotationMoved(TmGeoAnnotation annotation);
    void annotationRadiusUpdated(TmGeoAnnotation annotation);
}
