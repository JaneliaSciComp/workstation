package org.janelia.it.workstation.gui.geometric_search.gl.mesh;

import java.nio.ByteBuffer;
import org.janelia.geometry3d.Matrix4;

import javax.media.opengl.GL4;
import java.nio.IntBuffer;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.gui.geometric_search.gl.GL4Shader;
import org.janelia.it.workstation.gui.geometric_search.gl.OITDrawShader;
import org.janelia.it.workstation.gui.geometric_search.viewer.GL4TransparencyContext;
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
