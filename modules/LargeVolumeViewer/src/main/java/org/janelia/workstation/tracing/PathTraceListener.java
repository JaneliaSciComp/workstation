package org.janelia.workstation.tracing;

/**
 * Implement this to hear about new anchored voxel paths.
 * @author fosterl
 */
public interface PathTraceListener {
    void pathTraced(Long neuronId, AnchoredVoxelPath path);
}
