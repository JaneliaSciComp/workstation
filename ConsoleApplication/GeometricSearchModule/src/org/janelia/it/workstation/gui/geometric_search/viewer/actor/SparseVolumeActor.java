package org.janelia.it.workstation.gui.geometric_search.viewer.actor;

import org.janelia.geometry3d.Vector4;
import java.util.List;

/**
 * Created by murphys on 8/6/2015.
 */
public class SparseVolumeActor extends DenseVolumeActor {

    public SparseVolumeActor(String name, List<Vector4> voxels, float xSize, float ySize, float zSize, float voxelUnitSize) {
        super(name, voxels, xSize, ySize, zSize, voxelUnitSize);
    }

}
