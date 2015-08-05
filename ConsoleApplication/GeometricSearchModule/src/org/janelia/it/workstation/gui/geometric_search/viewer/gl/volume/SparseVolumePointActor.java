/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.geometric_search.viewer.gl.volume;

import java.io.File;
import java.nio.FloatBuffer;
import javax.media.opengl.GL4;
import org.janelia.geometry3d.Matrix4;
import org.janelia.it.workstation.gui.viewer3d.VolumeDataAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author murphys
 */
public class SparseVolumePointActor extends SparseVolumeBaseActor implements VolumeDataAcceptor
{
    private final Logger logger = LoggerFactory.getLogger(SparseVolumePointActor.class);

    public SparseVolumePointActor(File volumeFile, int volumeChannel, float volumeCutoff) {
        super(volumeFile, volumeChannel, volumeCutoff);
    }
    
    public void setVertexRotation(Matrix4 rotation) {
        this.vertexRotation=rotation;
    }

    @Override
    public void display(GL4 gl) {
        super.display(gl);

        checkGlError(gl, "d super.display() error");
        gl.glBindVertexArray(vertexArrayId.get(0));
        checkGlError(gl, "d glBindVertexArray error");
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId.get(0));
        checkGlError(gl, "d glBindBuffer error");
        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0);
        gl.glVertexAttribPointer(1, 3, GL4.GL_FLOAT, false, 0, viList.size() * 3 * 4);
        checkGlError(gl, "d glVertexAttribPointer error");
        gl.glEnableVertexAttribArray(0);
        checkGlError(gl, "d glEnableVertexAttribArray 0 error");
        gl.glEnableVertexAttribArray(1);
        checkGlError(gl, "d glEnableVertexAttribArray 1 error");
        gl.glDrawArrays(GL4.GL_POINTS, 0, viList.size());
        checkGlError(gl, "d glDrawArrays error");
    }

    @Override
    public void init(GL4 gl) {

        super.init(gl);

        // We want to create a triangle for each face, picking from the vertices
        FloatBuffer fb=FloatBuffer.allocate(viList.size()*3*2); // 3 floats per vertex, and 3 floats for the normal data, which we need for shader compatibility
             
        // First, vertex information
        for (int v=0;v<viList.size();v++) {
            viGroup vg=viList.get(v);
            fb.put(v*3,vg.x);
            fb.put(v*3+1,vg.y);
            fb.put(v*3+2,vg.z);
        }
        
        // Second, use first-two floats as flag for vertex shader to use last float as intensity value
        int v2offset=viList.size()*3;
        for (int v=0;v<viList.size();v++) {
            viGroup vg=viList.get(v);
            float fc=2000000.0f;
            fb.put(v2offset+v*3,fc);
            fb.put(v2offset+v*3+1,fc);
            fb.put(v2offset+v*3+2,vg.w);
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
        super.dispose(gl);
    }

}
