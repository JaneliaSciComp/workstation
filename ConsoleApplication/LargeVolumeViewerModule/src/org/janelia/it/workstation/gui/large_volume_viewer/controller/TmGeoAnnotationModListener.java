/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import java.util.List;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;

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
