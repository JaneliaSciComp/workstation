package org.janelia.it.workstation.gui.geometric_search.viewer.gl;

import javax.media.opengl.GL4;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by murphys on 4/22/15.
 */
public class GL4ShaderActionSequence {

    String name;
    private GL4Shader shader;
    boolean applyMemoryBarrier=false;
    List<GL4SimpleActor> actorSequence=new ArrayList<>();

    public GL4ShaderActionSequence(String name) {
        this.name=name;
    }

    public String getName() {
        return name;
    }

    public GL4Shader getShader() {
        return shader;
    }

    public void setShader(GL4Shader shader) {
        this.shader = shader;
    }

    public List<GL4SimpleActor> getActorSequence() {
        return actorSequence;
    }

    public void setActorSequence(List<GL4SimpleActor> actorSequence) {
        this.actorSequence = actorSequence;
    }

    public void dispose(GL4 gl) {
        for (GL4SimpleActor actor : actorSequence) {
            actor.dispose(gl);
        }
        shader.dispose(gl);
    }

    public void disposeAndClearActorsOnly(GL4 gl) {
        for (GL4SimpleActor actor : actorSequence) {
            actor.dispose(gl);
        }
        actorSequence.clear();
    }

    public void setApplyMemoryBarrier(boolean useBarrier) {
        this.applyMemoryBarrier=useBarrier;
    }


    public void init(GL4 gl) throws Exception {
        shader.init(gl);
        for (GL4SimpleActor actor : actorSequence) {
            actor.init(gl);
        }
    }

    public void display(GL4 gl) {
        shader.load(gl);


        shader.display(gl);

//        gl.glEnable(GL4.GL_DEPTH_TEST);
////        gl.glShadeModel(GL4.GL_SMOOTH);
////        gl.glDisable(GL4.GL_ALPHA_TEST);
////        gl.glAlphaFunc(GL4.GL_GREATER, 0.5f);
//        gl.glEnable(GL4.GL_BLEND);
//        gl.glBlendFunc(GL4.GL_SRC_ALPHA, GL4.GL_SRC_ALPHA);
//        gl.glBlendEquation(GL4.GL_FUNC_ADD);
//        gl.glDepthFunc(GL4.GL_LEQUAL);

        for (GL4SimpleActor actor: actorSequence) {
            if (actor.isVisible()) {
                actor.display(gl);
            }
        }

//        //gl.glEnable(GL4.GL_DEPTH_TEST);
//        gl.glDisable(GL4.GL_BLEND);
        
        if (applyMemoryBarrier) {
            gl.glMemoryBarrier(GL4.GL_ALL_BARRIER_BITS);
        }

        shader.unload(gl);
    }

}
