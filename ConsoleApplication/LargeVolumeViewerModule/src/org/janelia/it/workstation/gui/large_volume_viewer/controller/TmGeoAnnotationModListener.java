package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import java.util.List;

import org.janelia.it.jacs.model.domain.tiledMicroscope.TmGeoAnnotation;

/**
 * Implement this to be added as a listener for when TmGeoAnnotations are
 * modified.
 * 
 * @author fosterl
 */
public interface TmGeoAnnotationModListener {
    void annotationAdded(TmGeoAnnotation annotation);
    void annotationsDeleted(List<TmGeoAnnotation> annotations);
    void annotationReparented(TmGeoAnnotation annotation);
    void annotationNotMoved(TmGeoAnnotation annotation);
}
