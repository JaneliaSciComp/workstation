package org.janelia.it.workstation.ab2.gl;

import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GLShaderActionSequence {

    Logger logger= LoggerFactory.getLogger(GLShaderActionSequence.class);

    String name;
    private GLShaderProgram shader;
    boolean applyMemoryBarrier=false;
    List<GLAbstractActor> actorSequence=new ArrayList<>();

    public GLShaderActionSequence(String name) {
        this.name=name;
    }

    public String getName() {
        return name;
    }

    public GLShaderProgram getShader() {
        return shader;
    }

    public void setShader(GLShaderProgram shader) {
        this.shader = shader;
    }

    public List<GLAbstractActor> getActorSequence() {
        return actorSequence;
    }

    public void dispose(GL4 gl) {
        for (GLAbstractActor actor : actorSequence) {
            actor.dispose(gl, shader);
        }
        shader.dispose(gl);
        actorSequence.clear();
    }

    public void disposeAndClearActorsOnly(GL4 gl) {
        for (GLAbstractActor actor : actorSequence) {
            actor.dispose(gl, shader);
        }
        actorSequence.clear();
    }

    public void setApplyMemoryBarrier(boolean useBarrier) {
        this.applyMemoryBarrier=useBarrier;
    }


    public void init(GL4 gl) throws Exception {
        logger.info("init() start");
        GLAbstractActor.checkGlError(gl, "as0");
        shader.init(gl);
        GLAbstractActor.checkGlError(gl, "as1");
        logger.info("actorSequence contains "+actorSequence.size()+" actors");
        for (GLAbstractActor actor : actorSequence) {
//            actor.setMode(actorMode);
            GLAbstractActor.checkGlError(gl, "as actor pre");
            actor.init(gl, shader);
            GLAbstractActor.checkGlError(gl, "as actor post");
        }
        logger.info("init() done");
    }

    public void display(GL4 gl) {
        shader.load(gl);
        shader.display(gl);

//        gl.glEnable(GL4.GL_DEPTH_TEST);
//        gl.glShadeModel(GL4.GL_SMOOTH);
//        gl.glDisable(GL4.GL_ALPHA_TEST);
//        gl.glAlphaFunc(GL4.GL_GREATER, 0.5f);
//        gl.glEnable(GL4.GL_BLEND);
//        gl.glBlendFunc(GL4.GL_SRC_ALPHA, GL4.GL_SRC_ALPHA);
//        gl.glBlendEquation(GL4.GL_FUNC_ADD);
//        gl.glDepthFunc(GL4.GL_LEQUAL);

        for (GLAbstractActor actor: actorSequence) {
            //logger.info("actor.dislay() actor="+actor.getClass().getName());
            if (actor.getDisplay()) {
                actor.display(gl, shader);
            }
        }

        if (applyMemoryBarrier) {
            gl.glMemoryBarrier(GL4.GL_ALL_BARRIER_BITS);
        }
        shader.unload(gl);
    }

}
