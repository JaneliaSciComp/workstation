package org.janelia.workstation.gui.large_volume_viewer.listener;

import java.util.List;
import org.janelia.workstation.gui.large_volume_viewer.tracing.AnchoredVoxelPath;

/**
 * Event methods surrounding anchored voxel paths.
 * 
 * @author fosterl
 */
public interface AnchoredVoxelPathListener {
    void addAnchoredVoxelPath(AnchoredVoxelPath path);
    void addAnchoredVoxelPaths(List<AnchoredVoxelPath> paths);
    void removeAnchoredVoxelPath(AnchoredVoxelPath path);
    void removeAnchoredVoxelPaths(Long neuronID);
}
