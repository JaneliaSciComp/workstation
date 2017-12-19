package org.janelia.it.workstation.ab2.renderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import javax.media.opengl.GL4;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.Vector4;
import org.janelia.geometry3d.Viewport;
import org.janelia.it.workstation.ab2.controller.AB2Controller;
import org.janelia.it.workstation.ab2.controller.AB2ControllerMode;
import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.ab2.gl.GLAbstractActor;
import org.janelia.it.workstation.ab2.gl.GLShaderActionSequence;
import org.janelia.it.workstation.ab2.gl.GLShaderProgram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AB2RendererD extends AB2Renderer {
    Logger logger = LoggerFactory.getLogger(AB2RendererD.class);

    public static final double DISTANCE_TO_SCREEN_IN_PIXELS = 2500;
    protected static final double MAX_CAMERA_FOCUS_DISTANCE = 1000000.0;
    protected static final double MIN_CAMERA_FOCUS_DISTANCE = 0.001;
    public static final double DEFAULT_CAMERA_FOCUS_DISTANCE = 0.0;

    FloatBuffer backgroundColorBuffer=FloatBuffer.allocate(4);
    Vector4 backgroundColor=new Vector4(0.0f, 0.0f, 0.0f, 0.0f);

    protected Viewport viewport;
    protected Vantage vantage;

    protected List<GLShaderActionSequence> drawShaderList=new ArrayList<>();
    protected List<GLShaderActionSequence> pickShaderList=new ArrayList<>();

    public void addDrawShaderActionSequence(GLShaderActionSequence shaderActionSequence) {
        drawShaderList.add(shaderActionSequence);
    }

    public void addPickShaderActionSequence(GLShaderActionSequence shaderActionSequence) {
        pickShaderList.add(shaderActionSequence);
    }

    public GLShaderActionSequence getDrawShaderActionSequenceByIndex(int i) { return drawShaderList.get(i); }

    public GLShaderActionSequence getPickShaderActionSequenceByIndex(int i) { return pickShaderList.get(i); }

    Map<Integer, Vector4> colorIdMap=new HashMap<>();

    public Map<Integer, Vector4> getColorIdMap() { return colorIdMap; }

    protected boolean initialized=false;


    protected ConcurrentLinkedDeque<ImmutablePair<GLAbstractActor, GLShaderProgram>> actorDisposalQueue=new ConcurrentLinkedDeque<>();
    protected ConcurrentLinkedDeque<ImmutablePair<GLAbstractActor, GLShaderProgram>> actorInitQueue=new ConcurrentLinkedDeque<>();

//    protected ConcurrentLinkedDeque<MouseClickEvent> mouseClickEvents=new ConcurrentLinkedDeque<>();

//    protected class MouseClickEvent {
//        public int x=0;
//        public int y=0;
//        public MouseClickEvent(int x, int y) { this.x=x; this.y=y; }
//    }
//
//    public void addMouseClickEvent(int x, int y) {
//        mouseClickEvents.add(new MouseClickEvent(x,y));
//    }

    protected void setBackgroundColorBuffer() {
        backgroundColorBuffer.put(0,backgroundColor.get(0));
        backgroundColorBuffer.put(1,backgroundColor.get(1));
        backgroundColorBuffer.put(2,backgroundColor.get(2));
        backgroundColorBuffer.put(3,backgroundColor.get(3));
    }

    public void resetView() {
        boolean resetViewport=false;
        int viewportWidth=0;
        int viewportHeight=0;
        if (viewport!=null) {
            resetViewport=true;
            viewportWidth=viewport.getWidthPixels();
            viewportHeight=viewport.getHeightPixels();
        }
        vantage =new Vantage(null);
        viewport=new Viewport();
        viewport.setzNearRelative(0.1f);
        resetCamera();
        vantage.setFocus(0.0f,0.0f,(float)DEFAULT_CAMERA_FOCUS_DISTANCE);
        if (resetViewport) {
            viewport.setWidthPixels(viewportWidth);
            viewport.setHeightPixels(viewportHeight);
            viewport.getChangeObservable().notifyObservers();
        }
    }

    protected abstract void resetCamera();
    protected void checkGlError(GL4 gl, String message) {
        int errorNumber = gl.glGetError();
        if (errorNumber <= 0)
            return;
        String errorStr = glu.gluErrorString(errorNumber);
        logger.error( "OpenGL Error " + errorNumber + ": " + errorStr + ": " + message );
    }

    public void init(GL4 gl) {
        logger.info("init() start drawShaderList.size="+drawShaderList.size()+" pickShaderList.size="+pickShaderList.size());
        checkGlError(gl, "is0");
        try {
            for (GLShaderActionSequence shaderActionSequence : drawShaderList) {
                logger.info("shaderActionSequence init() called from drawShaderList");
                shaderActionSequence.init(gl);
            }

            for (GLShaderActionSequence shaderActionSequence : pickShaderList) {
                logger.info("shaderActionSequence init() called from pickShaderList");
                shaderActionSequence.init(gl);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void dispose(GL4 gl) {

        for (GLShaderActionSequence shaderActionSequence : drawShaderList) {
            shaderActionSequence.dispose(gl);
        }

        for (GLShaderActionSequence shaderActionSequence : pickShaderList) {
            shaderActionSequence.dispose(gl);
        }

    }

    public void clearActionSequenceActors(GLShaderActionSequence actionSequence) {
        List<GLAbstractActor> drawActors = actionSequence.getActorSequence();
        GLShaderProgram drawShader = actionSequence.getShader();
        for (GLAbstractActor drawActor : drawActors) {
            actorDisposalQueue.add(new ImmutablePair<>(drawActor, drawShader));
        }
        actionSequence.getActorSequence().clear();
    }

    protected abstract void resetVPMatrix();

    public void display(GL4 gl) {

        logger.info("displaySync() start");

        checkGlError(gl, "Check0.5");

        logger.info("past initial gl check");

        if (!initialized) {
            logger.info("Not initialized - returning");
            return;
        }

        if (actorDisposalQueue.size()>0) {
            logger.info("Starting actorDisposal loop");
            for (Pair<GLAbstractActor, GLShaderProgram> pair : actorDisposalQueue) {
                logger.info("Disposing actor in display()");
                GLAbstractActor actor=pair.getLeft();
                GLShaderProgram shader=pair.getRight();
                actor.dispose(gl, shader);
            }
            actorDisposalQueue.clear();
        }

        if (actorInitQueue.size()>0) {
            logger.info("Starting actorInit loop");
            for (Pair<GLAbstractActor, GLShaderProgram> pair : actorInitQueue) {
                logger.info("Initializing actor in display()");
                GLAbstractActor actor=pair.getLeft();
                GLShaderProgram shader=pair.getRight();
                actor.init(gl, shader);
            }
            actorInitQueue.clear();
        }

        // paint solid background color
//        gl.glDisable(GL4.GL_BLEND);
//        gl.glClearColor(0f, 0f, 0f, 1f);
//        gl.glClear(GL4.GL_COLOR_BUFFER_BIT);

        checkGlError(gl, "Check0");
        logger.info("Past Check0");

        gl.glClear(GL4.GL_DEPTH_BUFFER_BIT | GL4.GL_COLOR_BUFFER_BIT | GL4.GL_STENCIL_BUFFER_BIT);
        checkGlError(gl, "Check1");

        gl.glDisable(GL4.GL_DEPTH_TEST);
        checkGlError(gl, "Check2");
//        gl.glBlendEquation(GL4.GL_FUNC_ADD);
//        gl.glDisable(GL4.GL_BLEND);
        //gl.glDepthMask(true);

        //gl.glClearBufferfv(gl.GL_COLOR, 0, backgroundColorBuffer);

        //gl.glDisable(GL4.GL_BLEND);
        //checkGlError(gl, "Check3");



        gl.glEnable(GL4.GL_BLEND);
        checkGlError(gl, "Check7");

        gl.glBlendEquation(GL4.GL_MAX);
        checkGlError(gl, "Check8.1");
//        gl.glBlendEquation(GL4.GL_FUNC_ADD);
//        checkGlError(gl, "Check8.2");

//        gl.glBlendFunc(GL4.GL_ONE, GL4.GL_DST_ALPHA);
        //gl.glBlendFunc(GL4.GL_SRC_ALPHA, GL4.GL_ONE_MINUS_SRC_ALPHA);
        checkGlError(gl, "Check9");
//        gl.glBlendFunc(GL4.GL_SRC_ALPHA_SATURATE, GL4.GL_ONE);

//        gl.glDisable(GL4.GL_BLEND);

//        gl.glColorMask(true, true, true, true);
//        gl.glDepthMask(true);

//        gl.glDisable(GL4.GL_SCISSOR_TEST);
//        gl.glBindFramebuffer(GL4.GL_FRAMEBUFFER, 0);
//        checkGlError(gl, "Check4");
//        gl.glClearColor(0.0f, 0.2f, 0.0f, 1.0f);
//        checkGlError(gl, "Check5");
//        gl.glClearDepth(1.0f);
//        checkGlError(gl, "Check6");
//        gl.glClear(GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT | GL4.GL_STENCIL_BUFFER_BIT);
//        checkGlError(gl, "Check1");

//        gl.glFlush();

//        gl.glEnable(GL4.GL_BLEND);

        checkGlError(gl, "Check10");

        logger.info("past Check10");

        resetVPMatrix();

        for (GLShaderActionSequence shaderActionSequence : drawShaderList) {
            shaderActionSequence.display(gl);
        }

        checkGlError(gl, "Check11");

        logger.info("Beginning click event check");

        gl.glDisable(GL4.GL_BLEND);

        AB2ControllerMode mode=AB2Controller.getController().getCurrentMode();

        gl.glBindFramebuffer(GL4.GL_FRAMEBUFFER, mode.getPickFramebufferId());
        gl.glDrawBuffers(1, mode.getDrawBufferColorAttachment0());

        for (GLShaderActionSequence shaderActionSequence : pickShaderList) {
            shaderActionSequence.display(gl);
        }

        gl.glBindFramebuffer(GL4.GL_FRAMEBUFFER, 0);

        logger.info("Done with mouseClickEvents");

        checkGlError(gl, "Check12");
        logger.info("Done with displaySync()");
    }

    @Override
    public void reshape(GL4 gl, int x, int y, int width, int height, int screenWidth, int screenHeight) {
        //logger.info("reshape() x="+x+" y="+y+" width="+width+" height="+height+" screenWidth="+screenWidth+" screenHeight="+screenHeight);
        viewport.setHeightPixels(height);
        viewport.setWidthPixels(width);
        viewport.getChangeObservable().notifyObservers();
        if (initialized) {
            logger.info("initialized=true, calling display(gl) after reshape()");
            display(gl);
        }
    }

    @Override
    public void processEvent(AB2Event event) {

    }

}
