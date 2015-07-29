package org.janelia.it.workstation.gui.geometric_search.gl.oitarr;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.gui.geometric_search.gl.GL4Shader;
import org.janelia.it.workstation.gui.geometric_search.gl.GL4ShaderProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL4;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Created by murphys on 7/20/2015.
 */
public abstract class ArrayDrawShader extends GL4Shader {

    private Logger logger = LoggerFactory.getLogger(ArrayDrawShader.class);

    public ArrayDrawShader(GL4ShaderProperties properties) {
        super(properties);
    }

    protected ArrayTransparencyContext tc;

    public void setProjection(GL4 gl, Matrix4 projection) {
        setUniformMatrix4fv(gl, "proj", false, projection.asArray());
        checkGlError(gl, "ArrayDrawShader setProjection() error");
    }

    public void setView(GL4 gl, Matrix4 view) {
        setUniformMatrix4fv(gl, "view", false, view.asArray());
        checkGlError(gl, "ArrayDrawShader setView() error");
    }

    public void setModel(GL4 gl, Matrix4 model) {
        setUniformMatrix4fv(gl, "model", false, model.asArray());
        checkGlError(gl, "ArrayDrawShader setModel() error");
    }

    public void setDrawColor(GL4 gl, Vector4 drawColor) {
        setUniform4v(gl, "dcolor", 1, drawColor.toArray());
        checkGlError(gl, "ArrayDrawShader setDrawColor() error");
    }

    public void setMV(GL4 gl, Matrix4 mv) {
        setUniformMatrix4fv(gl, "mv", false, mv.asArray());
        checkGlError(gl, "ArrayDrawShader setMV() error");
    }

    public void setMVP(GL4 gl, Matrix4 mvp) {
        setUniformMatrix4fv(gl, "mvp", false, mvp.asArray());
        checkGlError(gl, "ArrayDrawShader setMVP() error");
    }

    public void setWidth(GL4 gl, int width) {
        setUniform(gl, "hpi_width", width);
    }

    public void setHeight(GL4 gl, int height) {
        setUniform(gl, "hpi_height", height);
    }

    public void setDepth(GL4 gl, int depth) {
        setUniform(gl, "hpi_depth", depth);
    }

    public void setTransparencyContext(ArrayTransparencyContext tc) {
        this.tc=tc;
    }

    @Override
    public void init(GL4 gl) throws ShaderCreationException {
        super.init(gl);
        checkGlError(gl, "ArrayDrawShader super.init() error");
    }

    @Override
    public void display(GL4 gl) {
        super.display(gl);
    }

}
