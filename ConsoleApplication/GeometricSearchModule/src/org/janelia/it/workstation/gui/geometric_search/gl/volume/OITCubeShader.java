package org.janelia.it.workstation.gui.geometric_search.gl.volume;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.gui.geometric_search.gl.OITDrawShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by murphys on 7/15/2015.
 */
public class OITCubeShader extends OITDrawShader {
    
    private Logger logger = LoggerFactory.getLogger( OITCubeShader.class );

    @Override
    public String getVertexShaderResourceName() {
        return "OITCubeShader_vertex.glsl";
    }
    
    @Override
    public String getGeometryShaderResourceName() {
        return "OITCubeShader_geometry.glsl";
    }

    @Override
    public String getFragmentShaderResourceName() {
        return "OITCubeShader_fragment.glsl";
    }
    
    public void setVoxelUnitSize(GL4 gl, Vector3 voxelUnitSize) {
        setUniform3v(gl, "voxelUnitSize", 1, voxelUnitSize.toArray());
        checkGlError(gl, "OITCubeShader setVoxelUnitSize() error");
    }

}
