package org.janelia.it.workstation.ab2.gl;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

public abstract class GLRegion implements GLEventListener {

    @Override
    public void init(GLAutoDrawable drawable) {

    }

    @Override
    public void dispose(GLAutoDrawable drawable) {

    }

    @Override
    public void display(GLAutoDrawable drawable) {

    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

    }

    public abstract void setMode(String modeName, int x0, int y0, int x1, int y1);

    public abstract String getMode();
}
