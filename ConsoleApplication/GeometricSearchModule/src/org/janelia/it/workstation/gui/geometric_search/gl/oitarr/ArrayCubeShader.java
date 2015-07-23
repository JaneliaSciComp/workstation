package org.janelia.it.workstation.gui.geometric_search.gl.oitarr;

import org.janelia.geometry3d.Vector3;
import org.janelia.it.workstation.gui.geometric_search.gl.OITDrawShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL4;

/**
 * Created by murphys on 7/20/2015.
 */
public class ArrayCubeShader extends ArrayDrawShader {

    private Logger logger = LoggerFactory.getLogger(ArrayCubeShader.class);

    @Override
    public String getVertexShaderResourceName() {
        return "ArrayCubeShader_vertex.glsl";
    }

    @Override
    public String getGeometryShaderResourceName() {
        return "ArrayCubeShader_geometry.glsl";
    }

    @Override
    public String getFragmentShaderResourceName() {
        return "ArrayCubeShader_fragment.glsl";
    }

    public void setVoxelUnitSize(GL4 gl, Vector3 voxelUnitSize) {
        setUniform3v(gl, "voxelUnitSize", 1, voxelUnitSize.toArray());
        checkGlError(gl, "ArrayCubeShader setVoxelUnitSize() error");
    }

    public void setWidth(GL4 gl, int width) {
        setUniform(gl, "hpi_width", width);
        checkGlError(gl, "ArrayCubeShader setWidth() error");
    }

    public void setHeight(GL4 gl, int height) {
        setUniform(gl, "hpi_height", height);
        checkGlError(gl, "ArrayCubeShader setHeight() error");
    }

    public void setDepth(GL4 gl, int depth) {
        setUniform(gl, "hpi_depth", depth);
        checkGlError(gl, "ArrayCubeShader setDepth() error");
    }

}
