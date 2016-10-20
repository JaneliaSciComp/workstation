package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import org.janelia.it.jacs.model.domain.tiledMicroscope.TmColorModel;
import org.janelia.it.jacs.shared.geom.Vec3;

/**
 * Implement this to handle changes to view state.
 * 
 * @author fosterl
 */
public interface ViewStateListener {
    void setCameraFocus(Vec3 focus);
    void centerNextParent();
    void loadColorModel(TmColorModel colorModel);
    void pathTraceRequested(Long id);
}
