package org.janelia.horta.actors;

import java.awt.Color;
import java.io.IOException;
import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.Matrix4;
import org.janelia.gltools.BasicShaderProgram;
import org.janelia.gltools.MeshActor;
import org.janelia.gltools.ShaderProgram;
import org.janelia.gltools.ShaderStep;
import org.janelia.gltools.material.BasicMaterial;
import org.janelia.gltools.texture.Texture2d;
import org.openide.util.Exceptions;

/**
 *
 * @author Christopher Bruns
 */
public class ConesMaterial extends BasicMaterial
{
    private static final int UNINITIALIZED_UNIFORM_INDEX = -1;
    // shader uniform parameter handles
    private int colorIndex = UNINITIALIZED_UNIFORM_INDEX;
    private int lightProbeIndex = UNINITIALIZED_UNIFORM_INDEX;
    private int radiusOffsetIndex = UNINITIALIZED_UNIFORM_INDEX;
    
    private final Texture2d lightProbeTexture;
    private final boolean manageLightProbeTexture;
    private final float[] color = new float[] {1, 0, 0, 1};
    private float minPixelRadius = 0.0f;

    public ConesMaterial(Texture2d lightProbeTexture, ShaderProgram conesShader) {
        if (conesShader == null)
            shaderProgram = new ConesShader();
        else
            shaderProgram = conesShader;
        if (lightProbeTexture == null) {
            manageLightProbeTexture = true;
            this.lightProbeTexture = new Texture2d();
            try {
                this.lightProbeTexture.loadFromPpm(getClass().getResourceAsStream(
                        "/org/janelia/gltools/material/lightprobe/"
                                + "Office1W165Both.ppm"));
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        else {
            manageLightProbeTexture = false;
            this.lightProbeTexture = lightProbeTexture;
        }
    }

    // Simplified display() method, with no load/unload, nor matrix manipulation.
    // So this can be in fast inner loop of multi-neuron render
    @Override 
    public void display(
                GL3 gl, 
                MeshActor mesh, 
                AbstractCamera camera,
                Matrix4 modelViewMatrix) 
    {
        displayMesh(gl, mesh, camera, modelViewMatrix);
    }
    
    // Override displayMesh() to display something other than triangles
    @Override
    protected void displayMesh(GL3 gl, MeshActor mesh, AbstractCamera camera, Matrix4 modelViewMatrix) {
        mesh.displayEdges(gl);
    }
    
    @Override
    public void dispose(GL3 gl) {
        super.dispose(gl);
        colorIndex = UNINITIALIZED_UNIFORM_INDEX;
        lightProbeIndex = UNINITIALIZED_UNIFORM_INDEX;
        if (manageLightProbeTexture)
            lightProbeTexture.dispose(gl);
        radiusOffsetIndex = UNINITIALIZED_UNIFORM_INDEX;
    }
    
    @Override
    public boolean hasPerFaceAttributes() {
        return false;
    }

    @Override
    public void init(GL3 gl) {
        super.init(gl);
        colorIndex = gl.glGetUniformLocation(
            shaderProgram.getProgramHandle(),
            "color");
        lightProbeIndex = gl.glGetUniformLocation(
            shaderProgram.getProgramHandle(),
            "lightProbe");
        if (manageLightProbeTexture)
            lightProbeTexture.init(gl);
        radiusOffsetIndex = gl.glGetUniformLocation(
            shaderProgram.getProgramHandle(),
            "radiusOffset");
    }

    // NOTE load and unload methods are not used, due to overridden display() method.
    // This class relies on some higher authority to set up the OpenGL state correctly.
    // (such as NeuronMPRenderer.AllSwcActor)
    @Override
    public void load(GL3 gl, AbstractCamera camera) {
        if (colorIndex == UNINITIALIZED_UNIFORM_INDEX) 
            init(gl);
        super.load(gl, camera);
        if (manageLightProbeTexture)
            lightProbeTexture.bind(gl, 0);
        gl.glUniform4fv(colorIndex, 1, color, 0);
        gl.glUniform1i(lightProbeIndex, 0); // use default texture unit, 0
        // radius offset depends on current zoom
        float micrometersPerPixel = 
            camera.getVantage().getSceneUnitsPerViewportHeight()
                / camera.getViewport().getHeightPixels();
        float radiusOffset = minPixelRadius * micrometersPerPixel;
        gl.glUniform1f(radiusOffsetIndex, radiusOffset);
    }
    
    @Override
    public void unload(GL3 gl) {
        super.unload(gl);
        if (manageLightProbeTexture)
            lightProbeTexture.unbind(gl);
    }
    
    @Override
    public boolean usesNormals() {
        return false;
    }
    
    public void setColor(Color color) {
        this.color[0] = color.getRed()/255f;
        this.color[1] = color.getGreen()/255f;
        this.color[2] = color.getBlue()/255f;
        this.color[3] = color.getAlpha()/255f;
        // Convert sRGB to linear-ish (RGB, but not alpha)
        for (int i = 0; i < 3; ++i)
            this.color[i] = this.color[i] * this.color[i];
    }
    
    public Color getColor()
    {
        return new Color(
                // convert linear to sRGB (but not alpha)
                (float)Math.sqrt(color[0]), 
                (float)Math.sqrt(color[1]), 
                (float)Math.sqrt(color[2]), 
                color[3]);
    }    

    void setMinPixelRadius(float minPixelRadius)
    {
        this.minPixelRadius = minPixelRadius;
    }

    float[] getColorArray()
    {
        return color;
    }

    float getMinPixelRadius()
    {
        return minPixelRadius;
    }
    
    public static class ConesShader extends BasicShaderProgram
    {
        public ConesShader()
        {
            try {
                getShaderSteps().add(new ShaderStep(GL3.GL_VERTEX_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "ConesVrtx330.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_GEOMETRY_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "imposter_fns330.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_GEOMETRY_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "ConesGeom330.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_FRAGMENT_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "imposter_fns330.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_FRAGMENT_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "ConesFrag330.glsl"))
                );
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }        
        }
    }
}
