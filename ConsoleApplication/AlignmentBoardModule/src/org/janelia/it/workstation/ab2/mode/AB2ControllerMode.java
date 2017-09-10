package org.janelia.it.workstation.ab2.mode;

import javax.media.opengl.GLEventListener;

import org.janelia.it.workstation.ab2.AB2Controller;
import org.janelia.it.workstation.ab2.renderer.AB2Basic3DRenderer;
import org.janelia.it.workstation.ab2.event.AB2Event;

public abstract class AB2ControllerMode implements GLEventListener {
    protected AB2Controller controller;

    public AB2ControllerMode(AB2Controller controller) {
        this.controller=controller;
    }

    public abstract void start();

    public abstract void stop();

    public abstract void shutdown();

    public abstract void processEvent(AB2Event event);

}
