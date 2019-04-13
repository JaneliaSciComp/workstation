package org.janelia.workstation.gui.large_volume_viewer.controller;

import org.janelia.workstation.tracing.AnchoredVoxelPath;

/**
 * Implement this to hear about new anchored voxel paths.
 * @author fosterl
 */
public interface PathTraceListener {
    void pathTraced(Long neuronId, AnchoredVoxelPath path);
}
