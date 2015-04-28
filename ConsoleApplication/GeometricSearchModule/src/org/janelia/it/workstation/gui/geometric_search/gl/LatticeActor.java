package org.janelia.it.workstation.gui.geometric_search.gl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL3;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Created by murphys on 4/28/15.
 */
public class LatticeActor extends GL3SimpleActor {

    private Logger logger = LoggerFactory.getLogger(LatticeActor.class);

    IntBuffer vertexArrayId=IntBuffer.allocate(1);
    IntBuffer vertexBufferId=IntBuffer.allocate(1);
    FloatBuffer fb;

    float[] varr;

    final int POINTS = 50;
    final float LENGTH = 100.0f;

    boolean loaded=false;

    @Override
    public void dispose(GL3 gl) {

    }

    @Override
    public void init(GL3 gl) {
        if (!loaded) {

            int arrSize=POINTS*POINTS*2;
            varr=new float[arrSize];
            float start=-1.0f*(LENGTH/2.0f);
            float d = LENGTH / POINTS;
            for (int i=0;i<POINTS;i++) {
                for (int j=0;j<POINTS;j++) {
                    float x = start + i*d;
                    float y = start + j*d;
                    int vs=(i*POINTS+j)*2;
                    varr[vs] = x;
                    varr[vs+1]=y;
                }
            }
            fb = FloatBuffer.allocate(arrSize);
            loaded=true;
        }
        gl.glGenVertexArrays(1, vertexArrayId);
        checkGlError(gl, "glGenVertexArrays error");
        gl.glBindVertexArray(vertexArrayId.get(0));
        checkGlError(gl, "glBindVertexArray error");
        gl.glGenBuffers(1, vertexBufferId);
        checkGlError(gl, "glGenBuffers error");
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vertexBufferId.get(0));
        checkGlError(gl, "glBindBuffer error");
        gl.glBufferData(GL3.GL_ARRAY_BUFFER, fb.capacity() * 4, fb, GL3.GL_STATIC_DRAW);
        checkGlError(gl, "glBufferData error");
    }

    @Override
    public void display(GL3 gl) {
        logger.info("display() ...");
        super.display(gl);
        gl.glPointSize(10f);
        gl.glBindVertexArray(vertexArrayId.get(0));
        checkGlError(gl, "glBindVertexArray error");
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vertexBufferId.get(0));
        checkGlError(gl, "glBindBuffer error");
        gl.glVertexAttribPointer(0, 2, GL3.GL_FLOAT, false, 0, 0);
        checkGlError(gl, "glVertexAttribPointer error");
        gl.glEnableVertexAttribArray(0);
        checkGlError(gl, "glEnableVertexAttribArray error");
        gl.glDrawArrays(GL3.GL_POINTS, 0, POINTS * POINTS);
        checkGlError(gl, "glDrawArrays error");
    }
}
