package org.janelia.it.workstation.gui.geometric_search.viewer.gl;

import org.janelia.geometry3d.Matrix4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL4;
import javax.media.opengl.glu.GLU;

/**
 * Created by murphys on 4/10/15.
 */
public abstract class GL4SimpleActor {

    protected static GLU glu = new GLU();
    private static Logger logger = LoggerFactory.getLogger(GL4SimpleActor.class);


    protected GLDisplayUpdateCallback updateCallback;

    protected Matrix4 model=new Matrix4();

    public abstract void dispose(GL4 gl);

    public abstract void init(GL4 gl);

    public void display(GL4 gl) {
        if (updateCallback!=null) {
            updateCallback.update(gl);
        }
    }

    public Matrix4 getModel() { return model; }

    public void setModel(Matrix4 model) { this.model=model; }

    public void setUpdateCallback(GLDisplayUpdateCallback updateCallback) {
        this.updateCallback=updateCallback;
    }

    protected void checkGlError(GL4 gl, String message) {
        int errorNumber = gl.glGetError();
        if (errorNumber <= 0)
            return;
        String errorStr = glu.gluErrorString(errorNumber);
        logger.error( "OpenGL Error " + errorNumber + ": " + errorStr + ": " + message );
    }

}
