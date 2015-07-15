package org.janelia.it.workstation.gui.geometric_search.gl.volume;

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
    public String getFragmentShaderResourceName() {
        return "OITCubeShader_fragment.glsl";
    }

}
