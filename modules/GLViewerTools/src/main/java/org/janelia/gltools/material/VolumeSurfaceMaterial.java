
package org.janelia.gltools.material;

import java.io.IOException;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.gltools.BasicShaderProgram;
import org.janelia.gltools.ShaderStep;
import org.janelia.gltools.texture.Texture3d;
import org.openide.util.Exceptions;

/**
 * Renders 3D texture on polygons at texture coordinate
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class VolumeSurfaceMaterial extends BasicMaterial 
{
    private final Texture3d volumeTexture;
    private int volumeTextureIndex = -1;
    
    public VolumeSurfaceMaterial(Texture3d volumeTexture) {
        this.volumeTexture = volumeTexture;
        shaderProgram = new VolumeSurfaceShader();
        setShadingStyle(Shading.FLAT);
    }

    @Override
    public void dispose(GL3 gl) {
        super.dispose(gl);
        volumeTexture.dispose(gl);
    }
    
    @Override
    public void load(GL3 gl, AbstractCamera camera) {
        super.load(gl, camera);
        int textureUnit = 0;
        volumeTexture.bind(gl, textureUnit);
        gl.glUniform1i(volumeTextureIndex, textureUnit);
    }
    
    @Override
    public void unload(GL3 gl) {
        super.unload(gl);
        volumeTexture.unbind(gl); // restore depth buffer writes
    }

    @Override
    public boolean usesNormals() {
        return false;
    }
    
    @Override
    public void init(GL3 gl) {
        super.init(gl);
        volumeTexture.init(gl);
        volumeTextureIndex = gl.glGetUniformLocation(
            shaderProgram.getProgramHandle(),
            "volumeTexture");
    }
    
    private static class VolumeSurfaceShader extends BasicShaderProgram {
        public VolumeSurfaceShader() {
            try {
                getShaderSteps().add(new ShaderStep(GL2ES2.GL_VERTEX_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/gltools/material/shader/"
                                        + "VolumeSurfaceVrtx.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL2ES2.GL_FRAGMENT_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/gltools/material/shader/"
                                        + "VolumeSurfaceFrag.glsl"))
                );
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }        
        }
    }
    
}
