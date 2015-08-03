package org.janelia.it.workstation.gui.geometric_search.viewer.gl;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL4;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Created by murphys on 7/14/2015.
 */
public abstract class OITDrawShader extends GL4Shader {

    private Logger logger = LoggerFactory.getLogger(OITDrawShader.class);

    private GL4TransparencyContext tc;

    public void setProjection(GL4 gl, Matrix4 projection) {
        setUniformMatrix4fv(gl, "proj", false, projection.asArray());
        checkGlError(gl, "OITDrawShader setProjection() error");
    }

    public void setView(GL4 gl, Matrix4 view) {
        setUniformMatrix4fv(gl, "view", false, view.asArray());
        checkGlError(gl, "OITDrawShader setView() error");
    }

    public void setModel(GL4 gl, Matrix4 model) {
        setUniformMatrix4fv(gl, "model", false, model.asArray());
        checkGlError(gl, "OITDrawShader setModel() error");
    }

    public void setDrawColor(GL4 gl, Vector4 drawColor) {
        setUniform4v(gl, "dcolor", 1, drawColor.toArray());
        checkGlError(gl, "OITDrawShader setDrawColor() error");
    }
    
    public void setMV(GL4 gl, Matrix4 mv) {
        setUniformMatrix4fv(gl, "mv", false, mv.asArray());
        checkGlError(gl, "OITDrawShader setMV() error");
    }
    
    public void setMVP(GL4 gl, Matrix4 mvp) {
        setUniformMatrix4fv(gl, "mvp", false, mvp.asArray());
        checkGlError(gl, "OITDrawShader setMVP() error");
    }

    public void setTransparencyContext(GL4TransparencyContext tc) {
        this.tc=tc;
    }

    @Override
    public void init(GL4 gl) throws ShaderCreationException {
        super.init(gl);
        checkGlError(gl, "OITDrawShader super.init() error");
    }

    @Override
    public void display(GL4 gl) {
        super.display(gl);
        checkGlError(gl, "d1 OITDrawShader super.display() error");
        
        if (tc==null) {
            // assume earlier instance of this class in the display sequence has been called
            return;
        }

        // Clear the headPointerTexture
        gl.glBindBuffer(GL4.GL_PIXEL_UNPACK_BUFFER, tc.getHeadPointerInitializerId());
        checkGlError(gl, "d2 OITDrawShader glBindBuffer() error");

        gl.glBindTexture(GL4.GL_TEXTURE_2D, tc.getHeadPointerTextureId());
        checkGlError(gl, "d3 OITDrawShader glBindTexture() error");

        gl.glTexSubImage2D(GL4.GL_TEXTURE_2D,
                0, // level
                0, // xoffset
                0, // yoffset
                GL4TransparencyContext.MAX_WIDTH,
                GL4TransparencyContext.MAX_HEIGHT,
                GL4.GL_RED_INTEGER,
                GL4.GL_UNSIGNED_INT,
                0);
        checkGlError(gl, "d4 OITDrawShader glTexImage2D() error");

        gl.glBindBuffer(GL4.GL_PIXEL_UNPACK_BUFFER, 0);

        // Bind the headPointerTexture for read-write
        gl.glBindImageTexture(1, tc.getHeadPointerTextureId(), 0, false, 0, GL4.GL_READ_WRITE, GL4.GL_R32UI);
        checkGlError(gl, "d5 OITDrawShader glBindImageTexture() error");

        // NEED TO CLEAN UP MESS, CLARIFY WHETHER USING TEXTURE OR BUFFER FOR THIS!!!!!
//        gl.glBindBuffer(GL4.GL_TEXTURE_BUFFER, tc.getFragmentStorageBufferId());
//        checkGlError(gl, "i8 GL4TransparencyContext glBindBuffer() error");

        // Bind the fragment list texture for read-write
//       gl.glBindImageTexture(0, tc.getFragmentStorageTextureId(), 0, false, 0, GL4.GL_READ_WRITE, GL4.GL_RGBA32UI);
//       checkGlError(gl, "d6 OITMeshDrawShader glBindImageTexture() error");

        // Bind and reset the atomic counter
        gl.glBindBufferBase(GL4.GL_ATOMIC_COUNTER_BUFFER, 0, tc.getAtomicCounterId());
        checkGlError(gl, "d6a OITDrawShader glBindBuffer() error");

        //      ByteBuffer bb = gl.glMapBufferRange(GL4.GL_ATOMIC_COUNTER_BUFFER, 0, 4, GL4.GL_MAP_WRITE_BIT | GL4.GL_MAP_INVALIDATE_BUFFER_BIT | GL4.GL_MAP_UNSYNCHRONIZED_BIT);
        ByteBuffer bb = gl.glMapBufferRange(GL4.GL_ATOMIC_COUNTER_BUFFER, 0, 4, GL4.GL_MAP_WRITE_BIT);
        checkGlError(gl, "d7a OITDrawShader glMapBufferRange() error");

        IntBuffer ib = bb.asIntBuffer();
        checkGlError(gl, "d8a OITDrawShader bb.asIntBuffer() error");

        ib.put(0, 0);
        checkGlError(gl, "d9a OITDrawShader ib.put() error");

        gl.glUnmapBuffer(GL4.GL_ATOMIC_COUNTER_BUFFER);
        checkGlError(gl, "d10a OITDrawShader glUnmapBuffer() error");

        gl.glBindBuffer(GL4.GL_ATOMIC_COUNTER_BUFFER, 0);

    }

}
