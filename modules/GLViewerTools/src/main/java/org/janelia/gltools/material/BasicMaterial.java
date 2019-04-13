
package org.janelia.gltools.material;

import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.Matrix4;
import org.janelia.gltools.MeshActor;
import org.janelia.gltools.ShaderProgram;

/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public abstract class BasicMaterial implements Material 
{
    protected ShaderProgram shaderProgram;
    private Shading shadingStyle = Shading.SMOOTH;
    protected boolean isInitialized = false;
    protected boolean cullFaces = true;
    // Uniform transforms
    protected int modelViewIndex = -1;
    protected int projectionIndex = -1;


    public void BasicMaterial() {}
    
    @Override 
    public void display(
                GL3 gl, 
                MeshActor mesh, 
                AbstractCamera camera,
                Matrix4 modelViewMatrix) 
    {
        load(gl, camera);
        displayWithMatrices(gl, mesh, camera, modelViewMatrix);
        unload(gl);
    }
    
    protected void displayNoMatrices(
                GL3 gl, 
                MeshActor mesh, 
                AbstractCamera camera,
                Matrix4 modelViewMatrix) 
    {
        activateCull(gl);
        displayMesh(gl, mesh, camera, modelViewMatrix);
    }
    
    protected void activateCull(GL3 gl) {
        if (cullFaces) {
            gl.glEnable(GL3.GL_CULL_FACE);
            gl.glCullFace(GL3.GL_BACK);    
        }
        else {
            gl.glDisable(GL3.GL_CULL_FACE);            
        }        
    }
    
    protected void displayWithMatrices(
                GL3 gl, 
                MeshActor mesh, 
                AbstractCamera camera,
                Matrix4 modelViewMatrix) 
    {
        Matrix4 projectionMatrix = camera.getProjectionMatrix();
        if (modelViewMatrix == null)
            modelViewMatrix = new Matrix4(camera.getViewMatrix());
        gl.glUniformMatrix4fv(modelViewIndex, 1, false, modelViewMatrix.asArray(), 0);
        gl.glUniformMatrix4fv(projectionIndex, 1, false, projectionMatrix.asArray(), 0);        
        displayNoMatrices(gl, mesh,camera, modelViewMatrix);
    }
    
    // Override displayMesh() to display something other than triangles
    protected void displayMesh(GL3 gl, 
            MeshActor mesh,
            AbstractCamera camera,
            Matrix4 modelViewMatrix) 
    {
        mesh.displayFaces(gl);
    }

    public boolean isCullFace() {
        return cullFaces;
    }

    @Override
    public void setCullFaces(boolean doCull) {
        this.cullFaces = doCull;
    }

    public ShaderProgram getShaderProgram() {
        return shaderProgram;
    }
    
    @Override
    public void init(GL3 gl) {
        if (isInitialized)
            return;
        if (shaderProgram == null)
            return;

        shaderProgram.init(gl);
        int s = shaderProgram.getProgramHandle();
        modelViewIndex = gl.glGetUniformLocation(s, "modelViewMatrix"); // -1 means no such item
        projectionIndex = gl.glGetUniformLocation(s, "projectionMatrix");
        
        isInitialized = true;
    }

    @Override
    public void load(GL3 gl, AbstractCamera camera) {
        if (! isInitialized) init(gl);
        if (shaderProgram == null) return;
        shaderProgram.load(gl);
    }

    @Override
    public void unload(GL3 gl) {
        if (shaderProgram == null) return;
        shaderProgram.unload(gl);
        gl.glPolygonMode(GL3.GL_FRONT_AND_BACK, GL3.GL_FILL);
    }

    public void setShaderProgram(ShaderProgram shaderProgram) {
        this.shaderProgram = shaderProgram;
    }

    public Shading getShadingStyle() {
        return shadingStyle;
    }

    public void setShadingStyle(Shading shadingStyle) {
        this.shadingStyle = shadingStyle;
    }

    @Override
    public int getShaderProgramHandle() {
        if (shaderProgram != null)
            return shaderProgram.getProgramHandle();
        else
            return 0;
    }

    @Override
    public boolean hasPerFaceAttributes() {
        return shadingStyle == Shading.FLAT;
    }

    @Override
    public void dispose(GL3 gl) {
        if (! isInitialized)
            return;
        if (shaderProgram != null)
            shaderProgram.dispose(gl);
        isInitialized = false;
    }

}
