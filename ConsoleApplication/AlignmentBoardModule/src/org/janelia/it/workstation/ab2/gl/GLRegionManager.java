package org.janelia.it.workstation.ab2.gl;

import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

public abstract class GLRegionManager implements GLEventListener {

    protected List<GLRegion> regions=new ArrayList<>();

    @Override
    public void init(GLAutoDrawable drawable) {
        for (GLRegion glRegion : regions) {
            glRegion.init(drawable);
        }
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        for (GLRegion glRegion : regions) {
            glRegion.dispose(drawable);
        }
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        for (GLRegion glRegion : regions) {
            glRegion.display(drawable);
        }
    }

    @Override
    public abstract void reshape(GLAutoDrawable drawable, int x, int y, int width, int height);

}
