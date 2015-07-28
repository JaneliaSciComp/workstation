package org.janelia.it.workstation.gui.geometric_search.gl.mesh;

import org.janelia.it.workstation.gui.geometric_search.gl.OITDrawShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by murphys on 5/15/15.
 */
public class OITMeshDrawShader extends OITDrawShader {
    
    private Logger logger = LoggerFactory.getLogger( OITMeshDrawShader.class );

    @Override
    public String getVertexShaderResourceName() {
        return "OITMeshDrawVertex.glsl";
    }

    @Override
    public String getFragmentShaderResourceName() {
        return "OITMeshDrawFragment.glsl";
    }

}
