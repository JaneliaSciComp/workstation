package org.janelia.it.workstation.gui.geometric_search.viewer.actor;

import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GL4SimpleActor;

import java.util.List;

/**
 * Created by murphys on 8/6/2015.
 */
public class DenseVolumeActor extends Actor {

    List<Vector4> voxels;

    public DenseVolumeActor(String name, List<Vector4> voxels) {
        this.name=name;
        this.voxels=voxels;
    }

    public GL4SimpleActor createAndSetGLActor() { return null; }


}
