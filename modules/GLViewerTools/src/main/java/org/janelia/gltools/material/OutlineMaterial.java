
package org.janelia.gltools.material;

import java.awt.Color;
import java.io.IOException;
import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.Matrix4;
import org.janelia.gltools.BasicShaderProgram;
import org.janelia.gltools.MeshActor;
import org.janelia.gltools.ShaderStep;
import org.openide.util.Exceptions;

/**
 * Colors mesh surface by normal direction
 * 
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class OutlineMaterial 
extends BasicMaterial
{
    private float lineWidth = 3.0f;
    private final float[] outlineColor = new float[]{0.10f, 0.10f, 0.10f, 1.0f};
    private int lineWidthIndex = -1;
    private int colorIndex = -1;
    
    public OutlineMaterial() {
        shaderProgram = new OutlineShader();
        setShadingStyle(Shading.NONE);
        setCullFaces(false); // outline requires not culling
    }
    
    @Override
    protected void displayMesh(GL3 gl, MeshActor mesh, AbstractCamera camera, Matrix4 modelViewMatrix) {
        setCullFaces(false); // no matter what the setting was before...
        mesh.displayTriangleAdjacencies(gl);
    }
    

    @Override 
    public void init(GL3 gl) {
        super.init(gl);
        lineWidthIndex = gl.glGetUniformLocation(
                shaderProgram.getProgramHandle(),
                "lineWidth");
        colorIndex = gl.glGetUniformLocation(
                shaderProgram.getProgramHandle(), 
                "outlineColor");
    }
    
    @Override
    public void load(GL3 gl, AbstractCamera camera) {
        super.load(gl, camera);
        float lineWidthSceneUnits = lineWidth
                * camera.getVantage().getSceneUnitsPerViewportHeight()
                / camera.getViewport().getHeightPixels();
        gl.glUniform1f(lineWidthIndex, lineWidthSceneUnits);
        gl.glUniform4fv(colorIndex, 1, outlineColor, 0);        
        // gl.glPolygonOffset(10.0f, 10.0f); // no effect?
        
        // For antialiasing
        gl.glEnable (GL.GL_BLEND); 
        gl.glBlendFunc (GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        // gl.glEnable(GL.GL_DEPTH_TEST); // READ the depth buffer
        // gl.glDepthMask(false); // But don't WRITE to the depth buffer
        
    }

    public void setOutlineColor(Color color) {
        outlineColor[0] = color.getRed()/255.0f;
        outlineColor[1] = color.getGreen()/255.0f;
        outlineColor[2] = color.getBlue()/255.0f;
        outlineColor[3] = color.getAlpha()/255.0f;
    }
    
    public void setOutlineWidth(float width) {
        this.lineWidth = width;
    }
    
    @Override
    public void unload(GL3 gl) {
        super.unload(gl);
        gl.glDepthMask(true); // restore depth buffer writes
    }

    @Override
    public boolean usesNormals() {
        return false;
    }

    private static class OutlineShader extends BasicShaderProgram {
        public OutlineShader() {
            try {
                getShaderSteps().add(new ShaderStep(GL2ES2.GL_VERTEX_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/gltools/material/shader/"
                                        + "OutlineVrtx.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_GEOMETRY_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/gltools/material/shader/"
                                        + "OutlineGeom.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL2ES2.GL_FRAGMENT_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/gltools/material/shader/"
                                        + "OutlineFrag.glsl"))
                );
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }
}
