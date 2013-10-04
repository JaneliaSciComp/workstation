package org.janelia.it.FlyWorkstation.gui.opengl;

import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;

import org.janelia.it.FlyWorkstation.gui.opengl.shader.BasicShader.ShaderCreationException;
import org.janelia.it.FlyWorkstation.gui.opengl.shader.MeshShader;

/**
 * Parent actor so multiple mesh actors can share a single shader
 * @author brunsc
 *
 */
public class MeshGroupActor extends CompositeGLActor
implements GL3Actor
{
    private MeshShader shader = new MeshShader();
    private int vertexLocation = 0;
    private int normalLocation = 1;
    private boolean bIsInitialized = false;
    
    @Override
    public void display(GLActorContext actorContext) {
        if (! bIsInitialized)
            init(actorContext);
        GL gl = actorContext.getGLAutoDrawable().getGL();
        if (gl.isGL2()) {
            // Don't use advanced shader with GL2...
            super.display(actorContext);
        }
        else {
            GL2GL3 gl2gl3 = gl.getGL2GL3();
            shader.load(gl2gl3);
            GL2Adapter gl2Adapter = actorContext.getGL2Adapter();
            float pr[] = gl2Adapter.getProjectionMatrix();
            shader.setUniformMatrix4fv(gl2gl3, "projection", false, pr);
            float mv[] = gl2Adapter.getModelViewMatrix();
            shader.setUniformMatrix4fv(gl2gl3, "modelView", false, mv);
            super.display(actorContext);
            shader.unload(gl2gl3);
        }
    }
    
    @Override
    public void dispose(GLActorContext actorContext) {
        GL2GL3 gl2gl3 = actorContext.getGLAutoDrawable().getGL().getGL2GL3();
        shader.dispose(gl2gl3);
        super.dispose(actorContext);
        bIsInitialized = false;
    }
    
    @Override
    public void init(GLActorContext actorContext) {
        GL2GL3 gl2gl3 = actorContext.getGLAutoDrawable().getGL().getGL2GL3();
        try {
            shader.init(gl2gl3);
            vertexLocation = gl2gl3.glGetAttribLocation(shader.getShaderProgram(), "vertex");
            normalLocation = gl2gl3.glGetAttribLocation(shader.getShaderProgram(), "normal");
        } catch (ShaderCreationException e) {
            e.printStackTrace();
        }
        for (GL3Actor actor : actors) {
            actor.init(actorContext); 
            if (actor instanceof MeshActor) {
                MeshActor ma = (MeshActor) actor;
                ma.setVertexLocation(vertexLocation);
                ma.setNormalLocation(normalLocation);
            }
        }
        bIsInitialized = true;
    }

}
