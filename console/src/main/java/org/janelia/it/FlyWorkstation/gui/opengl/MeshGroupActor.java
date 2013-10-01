package org.janelia.it.FlyWorkstation.gui.opengl;

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
    
    @Override
    public void display(GLActorContext actorContext) {
        GL2GL3 gl2gl3 = actorContext.getGLAutoDrawable().getGL().getGL2GL3();
        shader.load(gl2gl3);
        GL2Adapter gl2Adapter = actorContext.getGL2Adapter();
        float pr[] = gl2Adapter.getProjectionMatrix();
        shader.setUniformMatrix4fv(gl2gl3, "projection", false, pr);
        float mv[] = gl2Adapter.getModelViewMatrix();
        shader.setUniformMatrix4fv(gl2gl3, "modelView", false, mv);
        super.display(actorContext);
        shader.unload(gl2gl3);
        // for debugging
        /*
        System.out.print("Projection matrix: ");
        for (int i = 0; i < 16; ++i) {
            System.out.print(pr[i]);
            if (i < 15)
                System.out.print(", ");
            else
                System.out.println("");
        }
        System.out.print("Modelview matrix: ");
        for (int i = 0; i < 16; ++i) {
            System.out.print(mv[i]);
            if (i < 15)
                System.out.print(", ");
            else
                System.out.println("");
        }
        */
    }
    
    @Override
    public void dispose(GLActorContext actorContext) {
        GL2GL3 gl2gl3 = actorContext.getGLAutoDrawable().getGL().getGL2GL3();
        shader.dispose(gl2gl3);
        super.dispose(actorContext);
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
    }

}
