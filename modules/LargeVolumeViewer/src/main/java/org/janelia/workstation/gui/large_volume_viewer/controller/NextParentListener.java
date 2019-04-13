package org.janelia.workstation.gui.large_volume_viewer.controller;

import org.janelia.workstation.gui.large_volume_viewer.skeleton.Anchor;

/**
 * Implement this to hear about when the next parent is requested.
 * 
 * @author fosterl
 */
public interface NextParentListener {
    void setNextParent(Long id);
    void setNextParent(Anchor anchor);
}
