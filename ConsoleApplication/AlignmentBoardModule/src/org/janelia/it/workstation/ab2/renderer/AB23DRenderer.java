package org.janelia.it.workstation.ab2.renderer;

import javax.media.opengl.GL4;
import javax.media.opengl.glu.GLU;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AB23DRenderer implements AB2Renderer3DControls {
    Logger logger = LoggerFactory.getLogger(AB23DRenderer.class);
    protected static GLU glu = new GLU();


    protected void checkGlError(GL4 gl, String message) {
        int errorNumber = gl.glGetError();
        if (errorNumber <= 0)
            return;
        String errorStr = glu.gluErrorString(errorNumber);
        logger.error( "OpenGL Error " + errorNumber + ": " + errorStr + ": " + message );
    }

    public abstract void init(GL4 gl);

    public abstract void dispose(GL4 gl);

    public abstract void display(GL4 gl);

    public abstract void reshape(GL4 gl, int x, int y, int width, int height);

}
