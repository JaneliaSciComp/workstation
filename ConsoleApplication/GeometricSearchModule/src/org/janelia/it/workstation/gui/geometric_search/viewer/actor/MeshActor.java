package org.janelia.it.workstation.gui.geometric_search.viewer.actor;

import org.janelia.geometry3d.Matrix4;
import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerObjData;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GL4SimpleActor;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.oitarr.ArrayMeshGLActor;

/**
 * Created by murphys on 8/6/2015.
 */
public class MeshActor extends Actor {
    VoxelViewerObjData objData;
    Matrix4 vertexRotation;

    float edgeFalloff;
    float intensity;
    float ambience;

    public MeshActor(String name, VoxelViewerObjData objData, Matrix4 vertexRotation) {
        this.name=name;
        this.objData=objData;
        this.vertexRotation=vertexRotation;
    }

    @Override
    public GL4SimpleActor createAndSetGLActor() {
        if (objData==null) {
            return null;
        }
        ArrayMeshGLActor meshGLActor=new ArrayMeshGLActor(objData);
        if (vertexRotation!=null) meshGLActor.setVertexRotation(vertexRotation);
        glActor=meshGLActor;
        return glActor;
    }

    public float getEdgeFalloff() {
        return edgeFalloff;
    }

    public void setEdgeFalloff(float edgeFalloff) {
        this.edgeFalloff = edgeFalloff;
    }

    public float getIntensity() {
        return intensity;
    }

    public void setIntensity(float intensity) {
        this.intensity = intensity;
    }

    public float getAmbience() {
        return ambience;
    }

    public void setAmbience(float ambience) {
        this.ambience = ambience;
    }
}
