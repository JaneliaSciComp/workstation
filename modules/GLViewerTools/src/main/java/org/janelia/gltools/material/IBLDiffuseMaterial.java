
package org.janelia.gltools.material;

import java.io.IOException;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.gltools.BasicShaderProgram;
import org.janelia.gltools.ShaderStep;
import org.janelia.gltools.texture.Texture2d;
import org.openide.util.Exceptions;

/**
 * Diffuse material using Image Based Lighting (IBL)
 * A "light probe" 2D texture is used.
 * 
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class IBLDiffuseMaterial extends BasicMaterial 
{
    private Texture2d iblTexture;
    private int diffuseLightProbeIndex = -1;
    
    public IBLDiffuseMaterial() {
        try {
            shaderProgram = new IBLDiffuseShader();
            iblTexture = new Texture2d();
            iblTexture.loadFromPpm(getClass().getResourceAsStream(
                    "/org/janelia/gltools/material/lightprobe/"
                            + "Office1W165Both.ppm"));
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public void dispose(GL3 gl) {
        super.dispose(gl);
        iblTexture.dispose(gl);
    }
    
    @Override
    public void init(GL3 gl) {
        super.init(gl);
        iblTexture.init(gl);
        diffuseLightProbeIndex = gl.glGetUniformLocation(
            shaderProgram.getProgramHandle(),
            "diffuseLightProbe");
        // System.out.println("diffuseLightProbeIndex = "+diffuseLightProbeIndex);
    }
    
    @Override
    public void load(GL3 gl, AbstractCamera camera) {
        super.load(gl, camera);
        int textureUnit = 0;
        iblTexture.bind(gl, textureUnit);
        gl.glUniform1i(diffuseLightProbeIndex, textureUnit);
    }
    
    @Override
    public void unload(GL3 gl) {
        super.unload(gl);
        iblTexture.unbind(gl); // restore depth buffer writes
    }

    @Override
    public boolean usesNormals() {
        return true;
    }

    private static class IBLDiffuseShader extends BasicShaderProgram {

        public IBLDiffuseShader() {
            try {
                getShaderSteps().add(new ShaderStep(GL2ES2.GL_VERTEX_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/gltools/material/shader/"
                                        + "IBLDiffuseVrtx.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL2ES2.GL_FRAGMENT_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/gltools/material/shader/"
                                        + "IBLDiffuseFrag.glsl"))
                );
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }        
        }
        
    }
}
