
package org.janelia.gltools.material;

import java.awt.Color;
import java.io.IOException;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.gltools.BasicShaderProgram;
import org.janelia.gltools.ShaderStep;
import org.openide.util.Exceptions;

/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class TransparentEnvelope extends BasicMaterial
{
    private float[] diffuseColor = new float[] {1,0,1};
    private int colorIndex = -1;
    
    public TransparentEnvelope() {
        shaderProgram = new TransparentEnvelopeShader();
        setShadingStyle(Shading.SMOOTH);
        setCullFaces(false); // Show back faces
    }

    @Override
    public boolean usesNormals() {
        return true;
    }

    @Override
    public void init(GL3 gl) {
        if (isInitialized)
            return;
        super.init(gl);
        if (! isInitialized)
            return;
        int s = shaderProgram.getProgramHandle();
        colorIndex = gl.glGetUniformLocation(s, "diffuseColor");
    }

    @Override
    public void load(GL3 gl, AbstractCamera camera) {
        super.load(gl, camera);
        gl.glEnable(GL3.GL_BLEND);
        gl.glBlendFunc(GL3.GL_SRC_ALPHA, GL3.GL_ONE_MINUS_SRC_ALPHA);
        gl.glBlendEquation(GL3.GL_FUNC_ADD);
        gl.glDepthMask(false);
        gl.glUniform3fv(colorIndex, 1, diffuseColor, 0);
    }

    @Override
    public void unload(GL3 gl) {
        // gl.glDisable(GL3.GL_BLEND);
        gl.glDepthMask(true);
        super.unload(gl);
    }

    public void setDiffuseColor(Color color) {
        this.diffuseColor = color.getColorComponents(null);
    }

    private static class TransparentEnvelopeShader extends BasicShaderProgram {
        public TransparentEnvelopeShader() {
            try {
                getShaderSteps().add(new ShaderStep(
                        GL2ES2.GL_VERTEX_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/gltools/material/shader/"
                                        + "BasicMeshVrtx.glsl"))
                );
                getShaderSteps().add(new ShaderStep(
                        GL2ES2.GL_FRAGMENT_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/gltools/material/shader/"
                                        + "TransparentEnvelopeFrag.glsl"))
                );
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }
}
