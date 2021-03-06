
package org.janelia.gltools.material;

import javax.media.opengl.GL2ES2;
import org.janelia.gltools.BasicShaderProgram;
import org.janelia.gltools.ShaderStep;

/**
 * Colors mesh surface by normal direction
 * 
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class NormalMaterial 
extends BasicMaterial
{
    public NormalMaterial() {
        shaderProgram = new NormalShader();
        setShadingStyle(Shading.FLAT);
    }

    @Override
    public boolean usesNormals() {
        return true; // no kidding...
    }

    private static class NormalShader extends BasicShaderProgram {
        public NormalShader() {
            getShaderSteps().add(new ShaderStep(
                    GL2ES2.GL_VERTEX_SHADER, ""
                    + "#version 150 \n"
                    + " \n"
                    + "uniform mat4 modelViewMatrix = mat4(1); \n"
                    + "uniform mat4 projectionMatrix = mat4(1); \n"
                    + " \n"
                    + "in vec3 position; \n"
                    + "in vec3 normal; \n"
                    + " \n"
                    + "out vec3 fragNormal; \n"
                    + " \n"
                    + "void main(void) \n"
                    + "{ \n"
                    + "  gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1); \n"
                    // TODO - use Normal Matrix
                    + "  fragNormal = (modelViewMatrix * vec4(normal, 0)).xyz; \n"
                    + "} \n"
            ));
            getShaderSteps().add(new ShaderStep(
                    GL2ES2.GL_FRAGMENT_SHADER, ""
                    + "#version 150 \n"
                    + " \n"
                    + "in vec3 fragNormal; \n"
                    + " \n"
                    + "out vec4 fragColor; \n"
                    + " \n"
                    + "void main(void) \n"
                    + "{ \n"
                    + "  // convert normal component range from [-1,1] to [0,1] \n"
                    + "  vec3 n = normalize(fragNormal); \n"
                    + "  n = 0.5 * (n + vec3(1,1,1)); \n"
                    + "  fragColor = vec4(n, 1); \n"
                    + "} \n"
            ));
        }
    }
}
