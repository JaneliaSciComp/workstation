package org.janelia.it.workstation.gui.geometric_search.viewer;

import org.janelia.geometry3d.Matrix4;

import javax.media.opengl.GL3bc;

/**
 * Created by murphys on 4/10/15.
 */
public abstract class GL3SimpleActor {

    protected Matrix4 model;

    abstract void dispose(GL3bc gl);

    abstract void init(GL3bc gl);

    abstract void display(GL3bc gl, Matrix4 vp);

    public Matrix4 getModel() { return model; }

    public void setModel(Matrix4 model) { this.model=model; }

}
