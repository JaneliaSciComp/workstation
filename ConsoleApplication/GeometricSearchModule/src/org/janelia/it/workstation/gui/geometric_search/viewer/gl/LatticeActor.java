package org.janelia.it.workstation.gui.geometric_search.viewer.gl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL4;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Created by murphys on 4/28/15.
 */
public class LatticeActor extends GL4SimpleActor {

    private Logger logger = LoggerFactory.getLogger(LatticeActor.class);

    IntBuffer vertexArrayId=IntBuffer.allocate(1);
    IntBuffer vertexBufferId=IntBuffer.allocate(1);
    FloatBuffer fb;

    float[] varr;

    final int POINTS = 10;
    final float LENGTH = 1.0f;

    boolean loaded=false;

    @Override
    public void dispose(GL4 gl) {

    }

    @Override
    public void init(GL4 gl) {
        if (!loaded) {

            int arrSize=POINTS*POINTS*POINTS*3;
            varr=new float[arrSize];
            float start=-0.5f*LENGTH;
            float d = LENGTH / (1.0f * POINTS);

            for (int i=0;i<POINTS;i++) {
                for (int j=0;j<POINTS;j++) {
                    for (int k=0;k<POINTS;k++) {
                        float x = start + i * d;
                        float y = start + j * d;
                        float z = start+0.5f + k * d;
                        int vs = (i * POINTS * POINTS + j * POINTS + k) * 3;
                        varr[vs] = x;
                        varr[vs + 1] = y;
                        varr[vs + 2] = z;
                    }
                }
            }
            fb = FloatBuffer.allocate(arrSize);
            for (int f=0;f<arrSize;f++) {
                fb.put(f, varr[f]);
            }
            loaded=true;
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
    public void display(GL4 gl) {
        super.display(gl);
        gl.glPointSize(10f);
        gl.glBindVertexArray(vertexArrayId.get(0));
        checkGlError(gl, "glBindVertexArray error");
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId.get(0));
        checkGlError(gl, "glBindBuffer error");
        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0);
        checkGlError(gl, "glVertexAttribPointer error");
        gl.glEnableVertexAttribArray(0);
        checkGlError(gl, "glEnableVertexAttribArray error");
        gl.glDrawArrays(GL4.GL_POINTS, 0, POINTS * POINTS * POINTS);
        checkGlError(gl, "glDrawArrays error");
    }
}
