package org.janelia.it.workstation.ab2.controller;

import java.awt.Point;

import javax.media.opengl.GLEventListener;

import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.ab2.renderer.AB2Renderer;

public abstract class AB2ControllerMode implements GLEventListener {
    protected AB2Controller controller;

    public AB2ControllerMode(AB2Controller controller) {
        this.controller=controller;
    }

    public AB2Renderer getRendererAtPosition(Point point) {
        return null;
    }

    public abstract void start();

    public abstract void stop();

    public abstract void shutdown();

    public abstract void processEvent(AB2Event event);

}
