package org.janelia.it.workstation.gui.geometric_search.gl;

import javax.media.opengl.GL3;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by murphys on 4/22/15.
 */
public class GL3ShaderActionSequence {

    String name;
    private GL3Shader shader;
    List<GL3SimpleActor> actorSequence=new ArrayList<>();

    public GL3ShaderActionSequence(String name) {
        this.name=name;
    }

    public String getName() {
        return name;
    }

    public GL3Shader getShader(String name) {
        return shader;
    }

    public void setShader(GL3Shader shader) {
        this.shader = shader;
    }

    public List<GL3SimpleActor> getActorSequence() {
        return actorSequence;
    }

    public void setActorSequence(List<GL3SimpleActor> actorSequence) {
        this.actorSequence = actorSequence;
    }

    public void dispose(GL3 gl) {
        for (GL3SimpleActor actor : actorSequence) {
            actor.dispose(gl);
        }
        shader.dispose(gl);
    }


    public void init(GL3 gl) throws Exception {
        shader.init(gl);
        for (GL3SimpleActor actor : actorSequence) {
            actor.init(gl);
        }
    }

    public void display(GL3 gl) {
        shader.load(gl);


        shader.display(gl);

//        gl.glEnable(GL3.GL_DEPTH_TEST);
////        gl.glShadeModel(GL3.GL_SMOOTH);
////        gl.glDisable(GL3.GL_ALPHA_TEST);
////        gl.glAlphaFunc(GL3.GL_GREATER, 0.5f);
//        gl.glEnable(GL3.GL_BLEND);
//        gl.glBlendFunc(GL3.GL_SRC_ALPHA, GL3.GL_SRC_ALPHA);
//        gl.glBlendEquation(GL3.GL_FUNC_ADD);
//        gl.glDepthFunc(GL3.GL_LEQUAL);

        for (GL3SimpleActor actor: actorSequence) {
            actor.display(gl);
        }

//        //gl.glEnable(GL3.GL_DEPTH_TEST);
//        gl.glDisable(GL3.GL_BLEND);

        shader.unload(gl);
    }

}
