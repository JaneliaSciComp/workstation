package org.janelia.it.workstation.gui.geometric_search.viewer.gl.volume;

import java.io.File;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import javax.media.opengl.GL4;
import org.janelia.geometry3d.Matrix4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by murphys on 7/8/2015.
 */
public class SparseVolumeCubeActor extends SparseVolumeBaseActor
{
    private final Logger logger = LoggerFactory.getLogger(SparseVolumeCubeActor.class);

    public IntBuffer vertexArrayId= IntBuffer.allocate(1);
    public IntBuffer vertexBufferId=IntBuffer.allocate(1);

    public SparseVolumeCubeActor(File volumeFile, int volumeChannel, float volumeCutoff) {
        super(volumeFile, volumeChannel, volumeCutoff);
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
  
        // VERTEX
        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0);
        checkGlError(gl, "d glVertexAttribPointer error");
        gl.glEnableVertexAttribArray(0);
        checkGlError(gl, "d glEnableVertexAttribArray 0 error");
        
        // INTENSITY
        gl.glVertexAttribPointer(1, 1, GL4.GL_FLOAT, false, 0, viList.size() * 3 * 4);
        checkGlError(gl, "d glVertexAttribPointer error");
        gl.glEnableVertexAttribArray(1);
        checkGlError(gl, "d glEnableVertexAttribArray 1 error");       
        
        logger.info("display() calling glDrawArrays for GL4.GL_POINTS with viList.size="+viList.size());
        gl.glDrawArrays(GL4.GL_POINTS, 0, viList.size());
        checkGlError(gl, "d glDrawArrays error");
        
    }

    @Override
    public void init(GL4 gl) {

        super.init(gl);
        
        //viList.clear();
        
        //viGroup vg1 = new viGroup(0.0f, 0.0f, 0.0f, 1.0f);
        //viGroup vg2 = new viGroup(0.0f, 0.0f, 0.7f, 1.0f);
        
        //viList.add(vg1);
        //viList.add(vg2);

        FloatBuffer fb=FloatBuffer.allocate(viList.size()*4); // 3 floats per vertex, 1 for intensity
        
        logger.info("init() adding "+viList.size() +" vertices to FloatBuffer");
             
        // vertex information
        for (int v=0;v<viList.size();v++) {
            viGroup vg=viList.get(v);
            fb.put(v*3,vg.x);
            fb.put(v*3+1,vg.y);
            fb.put(v*3+2,vg.z);
        }
        
        // intensity information
        int intensityOffset = viList.size() * 3;
        for (int v=0;v<viList.size();v++) {
            viGroup vg=viList.get(v);
            fb.put(intensityOffset + v,vg.w);
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
