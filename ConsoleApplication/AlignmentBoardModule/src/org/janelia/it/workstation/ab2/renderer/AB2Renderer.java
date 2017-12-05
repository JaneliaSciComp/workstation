package org.janelia.it.workstation.ab2.renderer;

import javax.media.opengl.glu.GLU;

import org.janelia.it.workstation.ab2.event.AB2Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AB2Renderer {
    Logger logger = LoggerFactory.getLogger(AB2Renderer.class);
    protected static GLU glu = new GLU();

    public abstract void processEvent(AB2Event event);

}
