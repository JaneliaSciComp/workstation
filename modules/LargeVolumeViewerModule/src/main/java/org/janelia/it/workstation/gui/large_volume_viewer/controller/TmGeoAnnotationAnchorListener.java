package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import java.util.Collection;
import java.util.List;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;

/**
 * Implement this to hear events about TmGeoAnnotation encapsulated
 * Anchor information.
 * 
 * @author fosterl
 */
public interface TmGeoAnnotationAnchorListener {
    void anchorAdded(TmGeoAnnotation anchor);
    void anchorsAdded(List<TmGeoAnnotation> anchor);
    void anchorDeleted(TmGeoAnnotation anchor);
    void anchorReparented(TmGeoAnnotation anchor);
    void anchorMovedBack(TmGeoAnnotation anchor);
    void anchorMoved(TmGeoAnnotation anchor);
    void anchorRadiusChanged(TmGeoAnnotation anchor);
    void anchorRadiiChanged(List<TmGeoAnnotation> anchorList);
    void clearAnchors(Collection<TmGeoAnnotation> anchors);
    void clearAnchors();
}
