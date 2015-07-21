package org.janelia.it.workstation.gui.geometric_search.gl.oitarr;

import org.janelia.geometry3d.Vector3;
import org.janelia.it.workstation.gui.geometric_search.gl.GL4Shader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL4;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Created by murphys on 7/20/2015.
 */
public class ArraySortShader extends GL4Shader {

    private final Logger logger = LoggerFactory.getLogger(ArraySortShader.class);

    private ArrayTransparencyContext tc;

    public void setTransparencyContext(ArrayTransparencyContext tc) {
        this.tc=tc;
    }

    @Override
    public String getVertexShaderResourceName() {
        return "ArraySortShader_vertex.glsl";
    }

    @Override
    public String getFragmentShaderResourceName() {
        return "ArraySortShader_fragment.glsl";
    }

    IntBuffer quadDataBufferId=IntBuffer.allocate(1);
    FloatBuffer quadFb;
    IntBuffer vertexArrayId=IntBuffer.allocate(1);

    public void setWidth(GL4 gl, int width) {
        setUniform(gl, "hpi_width", width);
        checkGlError(gl, "ArraySortShader setWidth() error");
    }

    public void setHeight(GL4 gl, int height) {
        setUniform(gl, "hpi_height", height);
        checkGlError(gl, "ArraySortShader setHeight() error");
    }

    public void setDepth(GL4 gl, int depth) {
        setUniform(gl, "hpi_depth", depth);
        checkGlError(gl, "ArraySortShader setDepth() error");
    }

    @Override
    public void display(GL4 gl) {
        super.display(gl);
        checkGlError(gl, "d1 super.display() error");

        // Bind the headPointerTexture for read-write
        gl.glBindImageTexture(1, tc.getHeadPointerTextureId(), 0, false, 0, GL4.GL_READ_WRITE, GL4.GL_R32UI);
        checkGlError(gl, "d6.2 ArraySortShader glBindImageTexture() error");
        
        //gl.glBindBufferBase(GL4.GL_SHADER_STORAGE_BUFFER, 2, tc.getFragmentSSBOId());
        //checkGlError(gl, "d6.3 ArraySortShader glBindBufferBase() error");

        gl.glBindVertexArray(vertexArrayId.get(0));
        checkGlError(gl, "d7 ArraySortShader glBindVertexArray() error");

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, quadDataBufferId.get(0));
        checkGlError(gl, "d8 ArraySortShader glBindBuffer error");

        gl.glVertexAttribPointer(0, 4, GL4.GL_FLOAT, false, 0, 0);
        checkGlError(gl, "d9 ArraySortShader glVertexAttribPointer 0 () error");

        gl.glEnableVertexAttribArray(0);
        checkGlError(gl, "d10 ArraySortShader glEnableVertexAttribArray 0 () error");

        gl.glVertexAttribPointer(1, 2, GL4.GL_FLOAT, false, 0, 16 * 4);
        checkGlError(gl, "d11 ArraySortShader glVertexAttribPointer 1 () error");

        gl.glEnableVertexAttribArray(1);
        checkGlError(gl, "d12 ArraySortShader glEnableVertexAttribArray 1 () error");

        gl.glDrawArrays(GL4.GL_TRIANGLE_FAN, 0, 4);
        checkGlError(gl, "d13 ArraySortShader glDrawArrays() error");

    }

    @Override
    public void init(GL4 gl) throws ShaderCreationException {
        super.init(gl);
        gl.glUseProgram(getShaderProgram());

        // QUAD ////////////////////////

        quadFb = FloatBuffer.allocate(16 + 8);
        quadFb.put(0, -1.0f); quadFb.put(1, -1.0f); quadFb.put(2, 0.0f); quadFb.put(3, 1.0f);
        quadFb.put(4,  1.0f); quadFb.put(5, -1.0f); quadFb.put(6, 0.0f); quadFb.put(7, 1.0f);
        quadFb.put(8,  1.0f); quadFb.put(9,  1.0f); quadFb.put(10, 0.0f); quadFb.put(11, 1.0f);
        quadFb.put(12, -1.0f); quadFb.put(13, 1.0f); quadFb.put(14, 0.0f); quadFb.put(15, 1.0f);

        quadFb.put(16, 0.0f); quadFb.put(17, 0.0f);
        quadFb.put(18, 1.0f); quadFb.put(19, 0.0f);
        quadFb.put(20, 1.0f); quadFb.put(21, 1.0f);
        quadFb.put(22, 0.0f); quadFb.put(23, 1.0f);

        // VERTEX ARRAY
        gl.glGenVertexArrays(1, vertexArrayId);
        checkGlError(gl, "i1 ArraySortShader glGenVertexArrays error");

        gl.glBindVertexArray(vertexArrayId.get(0));
        checkGlError(gl, "i2 ArraySortShader glBindVertexArray error");

        gl.glGenBuffers(1, quadDataBufferId);
        checkGlError(gl, "i3 ArraySortShader glGenBuffers() error");

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, quadDataBufferId.get(0));
        checkGlError(gl, "i4 ArraySortShader glBindBuffer error");

        gl.glBufferData(GL4.GL_ARRAY_BUFFER, quadFb.capacity() * 4, quadFb, GL4.GL_STATIC_DRAW);
        checkGlError(gl, "i5 ArraySortShader glBufferData error");

    }

    @Override
    public void dispose(GL4 gl) {

    }
}
