/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import java.util.List;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;

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
    void clearAnchors();
}
