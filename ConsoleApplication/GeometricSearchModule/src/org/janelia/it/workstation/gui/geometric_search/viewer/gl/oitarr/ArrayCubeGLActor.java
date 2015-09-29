package org.janelia.it.workstation.gui.geometric_search.viewer.gl.oitarr;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GL4SimpleActor;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.volume.SparseVolumeBaseActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL4;
import java.io.File;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by murphys on 7/20/2015.
 */
public class ArrayCubeGLActor extends GL4SimpleActor
{
    private final Logger logger = LoggerFactory.getLogger(ArrayCubeGLActor.class);

    public IntBuffer vertexArrayId= IntBuffer.allocate(1);
    public IntBuffer vertexBufferId=IntBuffer.allocate(1);

    List<Vector4> voxels;
    float xSize;
    float ySize;
    float zSize;
    float getVoxelUnitSize;

    public Vector4 color=new Vector4(0.0f, 0.0f, 0.0f, 0.0f);

    public ArrayCubeGLActor(List<Vector4> voxels, float xSize, float ySize, float zSize, float voxelUnitSize) {
        this.voxels=voxels;
        this.xSize=xSize;
        this.ySize=ySize;
        this.zSize=zSize;
        this.getVoxelUnitSize=voxelUnitSize;
    }

    public Vector4 getColor() {
        return color;
    }

    public void setColor(Vector4 color) {
        this.color = color;
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

        // VERTEX
        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0);
        checkGlError(gl, "d glVertexAttribPointer error");
        gl.glEnableVertexAttribArray(0);
        checkGlError(gl, "d glEnableVertexAttribArray 0 error");

        // INTENSITY
        gl.glVertexAttribPointer(1, 1, GL4.GL_FLOAT, false, 0, voxels.size() * 3 * 4);
        checkGlError(gl, "d glVertexAttribPointer error");
        gl.glEnableVertexAttribArray(1);
        checkGlError(gl, "d glEnableVertexAttribArray 1 error");

        //logger.info("display() calling glDrawArrays for GL4.GL_POINTS with size="+voxels.size());
        gl.glDrawArrays(GL4.GL_POINTS, 0, voxels.size());
        checkGlError(gl, "d glDrawArrays error");
        
    }

    @Override
    public void init(GL4 gl) {

        logger.info("init() for ArrayCubeGLActor - size="+voxels.size());

        FloatBuffer fb=FloatBuffer.allocate(voxels.size()*4); // 3 floats per vertex, 1 for intensity

        // vertex information
        for (int v=0;v<voxels.size();v++) {
            Vector4 vg=voxels.get(v);
            float[] data=vg.toArray();
            fb.put(v*3,data[0]);
            fb.put(v*3+1,data[1]);
            fb.put(v*3+2,data[2]);
        }

        // intensity information
        int intensityOffset = voxels.size() * 3;
        for (int v=0;v<voxels.size();v++) {
            Vector4 vg=voxels.get(v);
            float[] data=vg.toArray();
            fb.put(intensityOffset + v, data[3]);
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

    public float getVoxelUnitSize() {
        return getVoxelUnitSize;
    }

}
