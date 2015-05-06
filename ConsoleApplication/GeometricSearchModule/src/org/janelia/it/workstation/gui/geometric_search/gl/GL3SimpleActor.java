package org.janelia.it.workstation.gui.geometric_search.gl;

import org.janelia.geometry3d.Matrix4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL3;
import javax.media.opengl.glu.GLU;

/**
 * Created by murphys on 4/10/15.
 */
public abstract class GL3SimpleActor {

    protected static GLU glu = new GLU();
    private static Logger logger = LoggerFactory.getLogger(GL3SimpleActor.class);


    protected GLDisplayUpdateCallback updateCallback;

    protected Matrix4 model=new Matrix4();

    public abstract void dispose(GL3 gl);

    public abstract void init(GL3 gl);

    public void display(GL3 gl) {
        if (updateCallback!=null) {
            updateCallback.update(gl);
        }
    }

    public Matrix4 getModel() { return model; }

    public void setModel(Matrix4 model) { this.model=model; }

    public void setUpdateCallback(GLDisplayUpdateCallback updateCallback) {
        this.updateCallback=updateCallback;
    }

    protected void checkGlError(GL3 gl, String message) {
        int errorNumber = gl.glGetError();
        if (errorNumber <= 0)
            return;
        String errorStr = glu.gluErrorString(errorNumber);
        logger.error( "OpenGL Error " + errorNumber + ": " + errorStr + ": " + message );
    }

}
