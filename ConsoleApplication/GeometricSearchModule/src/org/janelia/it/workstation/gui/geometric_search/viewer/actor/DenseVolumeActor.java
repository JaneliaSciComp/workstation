package org.janelia.it.workstation.gui.geometric_search.viewer.actor;

import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GL4SimpleActor;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.oitarr.ArrayCubeGLActor;

import java.util.List;

/**
 * Created by murphys on 8/6/2015.
 */
public class DenseVolumeActor extends Actor {

    List<Vector4> voxels;
    float xSize;
    float ySize;
    float zSize;
    float voxelUnitSize;
    ArrayCubeGLActor arrayCubeGLActor;

    float brightness=1.0f;
    float transparency=1.0f;

    public DenseVolumeActor(String name, List<Vector4> voxels, float xSize, float ySize, float zSize, float voxelUnitSize) {
        this.name=name;
        this.voxels=voxels;
        this.xSize=xSize;
        this.ySize=ySize;
        this.zSize=zSize;
        this.voxelUnitSize=voxelUnitSize;
    }

    public GL4SimpleActor createAndSetGLActor() {
        arrayCubeGLActor=new ArrayCubeGLActor(voxels, xSize, ySize, zSize, voxelUnitSize);
        this.glActor=arrayCubeGLActor;
        return arrayCubeGLActor;
    }

    public float getBrightness() {
        return brightness;
    }

    public void setBrightness(float brightness) {
        this.brightness = brightness;
    }

    public float getTransparency() {
        return transparency;
    }

    public void setTransparency(float transparency) {
        this.transparency = transparency;
    }

}
