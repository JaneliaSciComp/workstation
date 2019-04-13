package org.janelia.gltools;

import java.io.IOException;
import java.io.InputStream;
import javax.media.opengl.GL2ES2;
import org.janelia.gltools.material.BasicMaterial;
import org.janelia.gltools.material.Material;
import org.janelia.gltools.texture.Texture2d;
import org.openide.util.Exceptions;

/**
 *
 * @author Christopher Bruns
 */
public class LightingBlitActor extends BasicScreenBlitActor
{

    public LightingBlitActor(Texture2d screenTexture)
    {
        super(screenTexture);
    }
    
    @Override
    protected ShaderProgram createShaderProgram() 
    {
         // Simple shader to blit color buffer to screen
        BasicShaderProgram blitProgram = new BasicShaderProgram() {};
        try {
            // debug crash here
            String shaderPath = "/org/janelia/gltools/material/shader/"
                                            + "RenderPassVrtx.glsl";
            InputStream shaderStream = getClass().getResourceAsStream(shaderPath);
            ShaderStep shaderStep = new ShaderStep(GL2ES2.GL_VERTEX_SHADER, shaderStream);
            blitProgram.getShaderSteps().add(shaderStep);
            /*
            blitProgram.getShaderSteps().add(new ShaderStep(
                        GL2ES2.GL_VERTEX_SHADER, 
                        getClass().getResourceAsStream(
                                    "/org/janelia/gltools/material/shader/"
                                            + "RenderPassVrtx.glsl"))
            );
                    */
            blitProgram.getShaderSteps().add(new ShaderStep(
                        GL2ES2.GL_FRAGMENT_SHADER, 
                        getClass().getResourceAsStream(
                                    "/org/janelia/gltools/material/shader/"
                                            + "LightingFrag.glsl"))
            );
        } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
        }  
        return blitProgram;
    }
    
    @Override
    protected Material createMaterial() {
        BasicMaterial blitMaterial = new BlitMaterial();
        blitMaterial.setShaderProgram(createShaderProgram());
        blitMaterial.setShadingStyle(Material.Shading.FLAT);
        return blitMaterial;
    }

}
