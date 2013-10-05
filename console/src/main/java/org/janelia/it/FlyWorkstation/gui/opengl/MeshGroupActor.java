package org.janelia.it.FlyWorkstation.gui.opengl;

import java.util.List;
import java.util.Vector;

import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;

import org.janelia.it.FlyWorkstation.gui.opengl.shader.BasicShader;
import org.janelia.it.FlyWorkstation.gui.opengl.shader.BasicShader.ShaderCreationException;
import org.janelia.it.FlyWorkstation.gui.opengl.shader.Mesh120Shader;
import org.janelia.it.FlyWorkstation.gui.opengl.shader.Mesh150Shader;

/**
 * Parent actor so multiple mesh actors can share a single shader
 * @author brunsc
 *
 */
public class MeshGroupActor extends CompositeGLActor
implements GL3Actor
{
	// Different shaders appropriate for GL2 and GL3...
    private Mesh120Shader shader120 = new Mesh120Shader();
    private Mesh150Shader shader150 = new Mesh150Shader();
    private BasicShader shader = shader120;
    private int vertexLocation = 0;
    private int normalLocation = 1;
    private boolean bIsInitialized = false;
    
    @Override
    public void display(GLActorContext actorContext) {
        if (! bIsInitialized)
            init(actorContext);
        // Some meshes need a shader, other forbid it
        List<GL3Actor> shaderActors = new Vector<GL3Actor>();
        GL gl = actorContext.getGLAutoDrawable().getGL();
        for (GL3Actor actor : actors) {
        	if (gl.isGL3()) {
        		shaderActors.add(actor);
        		continue;
        	}
        	if (actor instanceof MeshActor) {
        		MeshActor meshActor = (MeshActor) actor;
        		if (meshActor.getDisplayMethod() == MeshActor.DisplayMethod.VBO_WITH_SHADER) {
        			shaderActors.add(actor);
        			continue;
        		}
        	}
            actor.display(actorContext);
        }
        if (shaderActors.size() > 0) {
            GL2GL3 gl2gl3 = gl.getGL2GL3();
            // TODO - have shader pull its own information from gl2Adapter
            shader.load(gl2gl3);
            GL2Adapter gl2Adapter = actorContext.getGL2Adapter();
            float pr[] = gl2Adapter.getProjectionMatrix();
            shader.setUniformMatrix4fv(gl2gl3, "projection", false, pr);
            float mv[] = gl2Adapter.getModelViewMatrix();
            shader.setUniformMatrix4fv(gl2gl3, "modelView", false, mv);

            for (GL3Actor actor : shaderActors)
            	actor.display(actorContext);
            
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
    	GL gl = actorContext.getGLAutoDrawable().getGL();
    	if (gl.isGL2())
    		shader = shader120;
    	else
    		shader = shader150;
        GL2GL3 gl2gl3 = gl.getGL2GL3();
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
