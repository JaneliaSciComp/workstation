package org.janelia.it.workstation.gui.geometric_search.gl;

import org.janelia.geometry3d.Matrix4;

import javax.media.opengl.GL3;

/**
 * Created by murphys on 4/10/15.
 */
public abstract class GL3SimpleActor {

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

}
