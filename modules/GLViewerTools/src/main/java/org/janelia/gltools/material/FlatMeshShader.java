
package org.janelia.gltools.material;

import javax.media.opengl.GL2ES2;
import org.janelia.gltools.BasicShaderProgram;
import org.janelia.gltools.ShaderStep;

/**
 * A single solid color, with no lighting effects.
 *
 * @author brunsc
 */
public class FlatMeshShader extends BasicShaderProgram {
    private final float[] color = {0,0,0};

    /**
     * 
     * @param red red color component; range zero to one
     * @param green green color component; range zero to one
     * @param blue  blue color component; range zero to one
     */
    public FlatMeshShader(float red, float green, float blue) {
        color[0] = red;
        color[1] = green;
        color[2] = blue;

        getShaderSteps().add(new ShaderStep(
                GL2ES2.GL_VERTEX_SHADER, ""
                + "#version 150\n"
                + "\n"
                + "uniform mat4 modelViewMatrix = mat4(1);"
                + "uniform mat4 projectionMatrix = mat4(1);"
                + "\n"
                + "in vec3 position; \n"
                + "\n"
                + "void main(void)\n"
                + "{\n"
                + "  gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1);\n"
                + "}\n")
        );

        getShaderSteps().add(new ShaderStep(
                GL2ES2.GL_FRAGMENT_SHADER, ""
                + "#version 150\n"
                + "\n"
                + "out vec4 fragColor; \n"
                + "\n"
                + "void main(void)\n"
                + "{\n"
                + "  fragColor = vec4("
                + color[0] + ", " // red
                + color[1] + ", " // green
                + color[2] + ", 1); \n" // blue
                + "}\n")
        );
    }
}
