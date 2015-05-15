package org.janelia.it.workstation.gui.geometric_search.gl;

import javax.media.opengl.GL4;
import java.nio.IntBuffer;

/**
 * Created by murphys on 5/15/15.
 */
public class OITMeshDrawShader extends GL4Shader {

    static final int MAX_HEIGHT = 2048;
    static final int MAX_WIDTH = 2048;
    int headPointerTotalPixels=0;

    IntBuffer headPointerId = IntBuffer.allocate(1);
    IntBuffer headPointerInitializerId = IntBuffer.allocate(1);
    IntBuffer atomicCounterId = IntBuffer.allocate(1);

    @Override
    public String getVertexShaderResourceName() {
        return "OITMeshDrawShaderVertex.glsl";
    }

    @Override
    public String getFragmentShaderResourceName() {
        return "OITMeshDrawFragment.glsl";
    }

    @Override
    public void init(GL4 gl) throws ShaderCreationException {
        super.init(gl);
        headPointerTotalPixels=MAX_HEIGHT * MAX_WIDTH;

        // Allocate empty texture of correct size
        gl.glGenTextures(1, headPointerId);
        gl.glBindTexture(GL4.GL_TEXTURE_2D, headPointerId.get(0));
        gl.glTexImage2D(GL4.GL_TEXTURE_2D, 0,
                GL4.GL_R32UI,
                MAX_HEIGHT,
                MAX_WIDTH,
                0,
                GL4.GL_RED_INTEGER,
                GL4.GL_UNSIGNED_INT,
                0);

        // Create PBO from which to clear the headPointerTexture
        IntBuffer hpiData = IntBuffer.allocate(headPointerTotalPixels);
        for (int i=0;i<hpiData.capacity();i++) {
            hpiData.put(i, Integer.MAX_VALUE);
        }

        gl.glGenBuffers(1, headPointerInitializerId);
        gl.glBindBuffer(GL4.GL_PIXEL_UNPACK_BUFFER, headPointerId.get(0));
        gl.glBufferData(GL4.GL_PIXEL_UNPACK_BUFFER,
                headPointerTotalPixels * 4,
                hpiData,
                GL4.GL_STATIC_DRAW);

        // Create atomic counter for next head pointer position
        gl.glGenBuffers(1, atomicCounterId);
        //gl.glBindBuffer(GL4.GL_ATOM);


    }

}
