package org.janelia.it.workstation.gui.geometric_search.viewer.gl.oitarr;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerObjData;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GL4SimpleActor;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.mesh.MeshObjActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL4;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by murphys on 7/25/2015.
 */
public class ArrayMeshGLActor extends GL4SimpleActor
{
    private final Logger logger = LoggerFactory.getLogger(ArrayMeshGLActor.class);

    IntBuffer vertexArrayId=IntBuffer.allocate(1);
    IntBuffer vertexBufferId=IntBuffer.allocate(1);
    Matrix4 vertexRotation=null;
    VoxelViewerObjData objData;
    int faceCount=0;

    public ArrayMeshGLActor(VoxelViewerObjData objData) {
        this.objData=objData;
    }
    public void setVertexRotation(Matrix4 rotation) {
        this.vertexRotation=rotation;
    }

    @Override
    public void display(GL4 gl) {
        super.display(gl);

        gl.glDisable(GL4.GL_DEPTH_TEST);

        checkGlError(gl, "d super.display() error");
        gl.glBindVertexArray(vertexArrayId.get(0));
        checkGlError(gl, "d glBindVertexArray error");
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId.get(0));
        checkGlError(gl, "d glBindBuffer error");
        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0);
        gl.glVertexAttribPointer(1, 3, GL4.GL_FLOAT, false, 0, faceCount * 9 * 4);
        checkGlError(gl, "d glVertexAttribPointer error");
        gl.glEnableVertexAttribArray(0);
        checkGlError(gl, "d glEnableVertexAttribArray 0 error");
        gl.glEnableVertexAttribArray(1);
        checkGlError(gl, "d glEnableVertexAttribArray 1 error");

        logger.info("calling glDrawArrays for vertexList.size()=" + faceCount);

        gl.glDrawArrays(GL4.GL_TRIANGLES, 0, faceCount*3);
        checkGlError(gl, "d glDrawArrays error");

    }

    @Override
    public void init(GL4 gl) {

        // We want to create a triangle for each face, picking from the vertices
        faceCount=objData.faceList.size()/3;
        FloatBuffer fb=FloatBuffer.allocate(faceCount*9*2); // 9 floats per 3 vertices, and 9 floats for the vertex normals
        // First, the vertices
        for (int f=0;f<faceCount;f++) {
            int[] fa=new int[3];
            fa[0]=objData.faceList.get(f*3)-1;
            fa[1]=objData.faceList.get(f*3+1)-1;
            fa[2]=objData.faceList.get(f*3+2)-1;
            for (int i=0;i<3;i++) {
                Vector3 vg=objData.vertexList.get(fa[i]);
                float[] vgData=vg.toArray();
                float x=vgData[0];
                float y=vgData[1];
                float z=vgData[2];
                if (vertexRotation!=null) {
                    Vector4 v = new Vector4(x, y, z, 1.0f);
                    Vector4 vr = vertexRotation.multiply(v);
                    x=vr.get(0);
                    y=vr.get(1);
                    z=vr.get(2);
                }
                int s=f*9+i*3;
                fb.put(s, x);
                fb.put(s+1, y);
                fb.put(s+2, z);
            }
        }
        // Next, the normals
        int vOffset=faceCount*9;
        for (int f=0;f<faceCount;f++) {
            int[] fa=new int[3];
            fa[0]=objData.faceList.get(f*3)-1;
            fa[1]=objData.faceList.get(f*3+1)-1;
            fa[2]=objData.faceList.get(f*3+2)-1;
            for (int i=0;i<3;i++) {
                Vector3 vg=objData.normalList.get(fa[i]);
                float[] vgData=vg.toArray();
                float x=vgData[0];
                float y=vgData[1];
                float z=vgData[2];
                int s=vOffset+f*9+i*3;
                fb.put(s, x);
                fb.put(s+1, y);
                fb.put(s+2, z);
            }
        }

        gl.glGenVertexArrays(1, vertexArrayId);
        checkGlError(gl, "glGenVertexArrays error");
        gl.glBindVertexArray(vertexArrayId.get(0));
        checkGlError(gl, "glBindVertexArray error");
        gl.glGenBuffers(1, vertexBufferId);
        checkGlError(gl, "glGenBuffers error");
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId.get(0));
        checkGlError(gl, "glBindBuffer error");
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, fb.capacity() * 4, fb, GL4.GL_STATIC_DRAW);
        checkGlError(gl, "glBufferData error");
    }

    @Override
    public void dispose(GL4 gl) {
        gl.glDeleteVertexArrays(1, vertexArrayId);
        gl.glDeleteBuffers(1, vertexBufferId);
    }

}
