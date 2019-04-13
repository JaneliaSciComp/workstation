
package org.janelia.gltools.material;

import javax.media.opengl.GL2ES2;
import org.janelia.gltools.BasicShaderProgram;
import org.janelia.gltools.ShaderStep;

/**
 * Colors mesh surface by normal direction
 * 
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class TexCoordMaterial 
extends BasicMaterial
{
    public TexCoordMaterial() {
        shaderProgram = new TexCoordShader();
        setShadingStyle(Shading.FLAT);
    }

    @Override
    public boolean usesNormals() {
        return false;
    }

    private static class TexCoordShader extends BasicShaderProgram {
        public TexCoordShader() {
            getShaderSteps().add(new ShaderStep(
                    GL2ES2.GL_VERTEX_SHADER, ""
                    + "#version 150 \n"
                    + " \n"
                    + "uniform mat4 modelViewMatrix = mat4(1); \n"
                    + "uniform mat4 projectionMatrix = mat4(1); \n"
                    + " \n"
                    + "in vec3 position; \n"
                    + "in vec3 texCoord; \n"
                    + " \n"
                    + "out vec3 fragTexCoord; \n"
                    + " \n"
                    + "void main(void) \n"
                    + "{ \n"
                    + "  gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1); \n"
                    + "  fragTexCoord = texCoord; \n"
                    + "} \n"
            ));
            getShaderSteps().add(new ShaderStep(
                    GL2ES2.GL_FRAGMENT_SHADER, ""
                    + "#version 150 \n"
                    + " \n"
                    + "in vec3 fragTexCoord; \n"
                    + " \n"
                    + "out vec4 fragColor; \n"
                    + " \n"
                    + "void main(void) \n"
                    + "{ \n"
                    + "  fragColor = vec4(fragTexCoord, 1); \n"
                    + "} \n"
            ));
        }
    }
}
