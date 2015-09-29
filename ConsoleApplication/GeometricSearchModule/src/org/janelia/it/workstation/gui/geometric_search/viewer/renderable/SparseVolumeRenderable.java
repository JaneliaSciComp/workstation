package org.janelia.it.workstation.gui.geometric_search.viewer.renderable;

import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.gui.geometric_search.viewer.actor.Actor;
import org.janelia.it.workstation.gui.geometric_search.viewer.actor.SparseVolumeActor;

import java.util.List;

/**
 * Created by murphys on 8/7/2015.
 */
public class SparseVolumeRenderable extends Renderable {

    List<Vector4> voxels;

    int xSize=0;
    int ySize=0;
    int zSize=0;
    float voxelSize=0.0f;

    public void init(int xsize, int ysize, int zsize, float voxelSize, List<Vector4> voxels) {
        this.xSize=xsize;
        this.ySize=ysize;
        this.zSize=zsize;
        this.voxelSize=voxelSize;
        this.voxels=voxels;
    }

    @Override
    public Actor createAndSetActor() {
        if (actor!=null) {
            disposeActor();
        }
        actor = new SparseVolumeActor(this.name, voxels, xSize, ySize, zSize, voxelSize);
        actor.setColor(preferredColor);
        return actor;
    }

    @Override
    public void disposeActor() {
        if (actor!=null) {
            actor.dispose();
        }
    }

    public List<Vector4> getVoxels() {
        return voxels;
    }

    public void setVoxels(List<Vector4> voxels) {
        this.voxels = voxels;
    }
}
