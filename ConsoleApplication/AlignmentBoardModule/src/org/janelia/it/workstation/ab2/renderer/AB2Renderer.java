package org.janelia.it.workstation.ab2.renderer;

import javax.media.opengl.GL4;
import javax.media.opengl.glu.GLU;

import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.ab2.gl.GLRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AB2Renderer {
    Logger logger = LoggerFactory.getLogger(AB2Renderer.class);

    GLRegion parentRegion;

    public GLRegion getParentRegion() {
        return parentRegion;
    }

    public AB2Renderer(GLRegion parentRegion) {
        this.parentRegion=parentRegion;
    }

    protected static GLU glu = new GLU();

    public abstract void processEvent(AB2Event event);

    public abstract void init(GL4 gl4);

    public abstract void dispose(GL4 gl4);

    public abstract void display(GL4 gl4);

    public abstract void reshape(GL4 gl, int x, int y, int width, int height, int screenWidth, int screenHeight);

}
