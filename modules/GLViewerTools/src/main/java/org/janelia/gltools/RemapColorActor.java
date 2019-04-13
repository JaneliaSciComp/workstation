
package org.janelia.gltools;

import java.io.IOException;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL3;
import org.janelia.console.viewerapi.model.ChannelColorModel;
import org.janelia.console.viewerapi.model.ImageColorModel;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.gltools.texture.Texture2d;
// import org.janelia.geometry3d.ChannelBrightnessModel;
import org.janelia.gltools.material.BasicMaterial;
import org.janelia.gltools.material.Material;
import org.openide.util.Exceptions;

/**
 * TODO - remove this class in favor of a RenderNode that uses 
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class RemapColorActor extends BasicScreenBlitActor {
    
    public RemapColorActor(Texture2d screenTexture, ImageColorModel colorMap) 
    {
        super(screenTexture);
        ((ColorMapMaterial)material).setBrightnessModel(colorMap);
    }
        
    @Override
    protected ShaderProgram createShaderProgram() 
    {
         // Simple shader to blit color buffer to screen
        BasicShaderProgram blitProgram = new BasicShaderProgram() {};
        try {
            blitProgram.getShaderSteps().add(new ShaderStep(
                        GL2ES2.GL_VERTEX_SHADER, 
                        getClass().getResourceAsStream(
                                    "/org/janelia/gltools/material/shader/"
                                            + "RenderPassVrtx.glsl"))
            );
            blitProgram.getShaderSteps().add(new ShaderStep(
                        GL2ES2.GL_FRAGMENT_SHADER, 
                        getClass().getResourceAsStream(
                                    "/org/janelia/gltools/material/shader/"
                                            + "ContrastFrag.glsl"))
            );
        } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
        }  
        return blitProgram;
    }
    
    @Override
    protected Material createMaterial() {
        BasicMaterial blitMaterial = new ColorMapMaterial();
        blitMaterial.setShaderProgram(createShaderProgram());
        blitMaterial.setShadingStyle(Material.Shading.FLAT);
        return blitMaterial;
    }
    
    static protected class ColorMapMaterial extends BlitMaterial 
    {
        private int opacityFunctionMinIndex = -1;
        private int opacityFunctionMaxIndex = -1;
        private int opacityFunctionGammaIndex = -1;
        private ImageColorModel brightnessModel;

        public void setBrightnessModel(ImageColorModel brightnessModel) {
            this.brightnessModel = brightnessModel;
        }

        @Override
        public void init(GL3 gl) {
            super.init(gl);
            opacityFunctionMinIndex = gl.glGetUniformLocation(
                shaderProgram.getProgramHandle(),
                "opacityFunctionMin");            
            opacityFunctionMaxIndex = gl.glGetUniformLocation(
                shaderProgram.getProgramHandle(),
                "opacityFunctionMax");            
            opacityFunctionGammaIndex = gl.glGetUniformLocation(
                shaderProgram.getProgramHandle(),
                "opacityFunctionGamma");            
        }

        @Override
        public void load(GL3 gl, AbstractCamera camera) {
            super.load(gl, camera);
            // TODO: Use per-channel brightness
            ChannelColorModel c0 = brightnessModel.getChannel(0);
            ChannelColorModel c1 = brightnessModel.getChannel(1);
            ChannelColorModel c2 = brightnessModel.getChannel(2);
            gl.glUniform3fv(opacityFunctionMinIndex, 1, new float[] {
                c0.getNormalizedMinimum(), 
                c1.getNormalizedMinimum(), 
                c2.getNormalizedMinimum()
            }, 0);
            gl.glUniform3fv(opacityFunctionMaxIndex, 1, new float[] {
                c0.getNormalizedMaximum(), 
                c1.getNormalizedMaximum(), 
                c2.getNormalizedMaximum()
            }, 0);
            gl.glUniform3fv(opacityFunctionGammaIndex, 1, new float[] {
                (float)c0.getGamma(), 
                (float)c1.getGamma(), 
                (float)c2.getGamma()
            }, 0);
        }
    }
}
