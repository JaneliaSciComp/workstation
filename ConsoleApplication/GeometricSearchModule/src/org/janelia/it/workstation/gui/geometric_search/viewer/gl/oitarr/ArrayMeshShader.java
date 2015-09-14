package org.janelia.it.workstation.gui.geometric_search.viewer.gl.oitarr;

import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GL4ShaderProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL4;

/**
 * Created by murphys on 7/25/2015.
 */
public class ArrayMeshShader extends ArrayDrawShader {

    private Logger logger = LoggerFactory.getLogger(ArrayMeshShader.class);

    public ArrayMeshShader(GL4ShaderProperties properties) {
        super(properties);
    }

    @Override
    public String getVertexShaderResourceName() {
        return "ArrayMeshShader_vertex.glsl";
    }

    @Override
    public String getFragmentShaderResourceName() {
        return "ArrayMeshShader_fragment.glsl";
    }

    @Override
    public void display(GL4 gl) {
        super.display(gl);
        checkGlError(gl, "d1 ArrayMeshShader super.display() error");
    }

    public void setEdgefalloff(GL4 gl, float edgefalloff) {
        setUniform(gl, "edgefalloff", edgefalloff);
        checkGlError(gl, "ArrayMeshShader setEdgefalloff() error");
    }

    public void setIntensity(GL4 gl, float intensity) {
        setUniform(gl, "intensity", intensity);
        checkGlError(gl, "ArrayMeshShader setIntensity() error");
    }

    public void setAmbience(GL4 gl, float ambience) {
        setUniform(gl, "ambience", ambience);
        checkGlError(gl, "ArrayMeshShader setAmbience() error");
    }

}
