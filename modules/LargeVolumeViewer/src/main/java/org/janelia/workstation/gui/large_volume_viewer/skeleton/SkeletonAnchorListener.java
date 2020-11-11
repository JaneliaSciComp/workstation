package org.janelia.workstation.gui.large_volume_viewer.skeleton;

/**
 * Implement this to hear about skeleton-package anchor events.
 * 
 * @author fosterl
 */
public interface SkeletonAnchorListener {
    void anchorMoved(Anchor anchor);
    void anchorMovedSilent(Anchor anchor);
}
