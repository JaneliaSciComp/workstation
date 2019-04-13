package org.janelia.gltools.material;

import javax.media.opengl.GL2ES2;
import org.janelia.gltools.BasicShaderProgram;
import org.janelia.gltools.ShaderStep;

/**
 * Paints a screen covering quad based on corner colors
 * @author Christopher Bruns
 */
public class ScreenGradientColorMaterial extends BasicMaterial {

    public ScreenGradientColorMaterial() {
        shaderProgram = new ScreenGradientColorShader();
    }
    
    @Override
    public boolean usesNormals() {
        return false;
    }

    private static class ScreenGradientColorShader extends BasicShaderProgram {

        public ScreenGradientColorShader() {
            getShaderSteps().add(new ShaderStep(
                    GL2ES2.GL_VERTEX_SHADER, ""
                    + "#version 330 \n"
                    + " \n"
                    + "in vec3 position; \n"
                    + "in vec4 color; \n"
                    + " \n"
                    + "out vec4 fragColor1; \n"
                    + " \n"
                    + "void main(void) \n"
                    + "{ \n"
                    + "  gl_Position = vec4(position, 1); \n"
                    + "  fragColor1 = color; \n"
                    + "} \n")
            );
            getShaderSteps().add(new ShaderStep(
                    GL2ES2.GL_FRAGMENT_SHADER, ""
                    + "#version 330 \n"
                    + " \n"
                    + "in vec4 fragColor1; \n"
                    + " \n"
                    + "out vec4 fragColor; \n"
                    + " \n"
                    + "void main(void) \n"
                    + "{ \n"
                    + "  fragColor = fragColor1; \n"
                    + "} \n")
            );
        }
    }
    
}
