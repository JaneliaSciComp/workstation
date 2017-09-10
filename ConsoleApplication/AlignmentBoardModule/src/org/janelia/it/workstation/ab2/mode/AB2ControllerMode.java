package org.janelia.it.workstation.ab2.mode;

import org.janelia.it.workstation.ab2.AB2Renderer;
import org.janelia.it.workstation.ab2.event.AB2Event;

public abstract class AB2ControllerMode {
    protected AB2Renderer renderer;

    public AB2ControllerMode(AB2Renderer renderer) {
        this.renderer=renderer;
    }

    public abstract void start();

    public abstract void stop();

    public abstract void shutdown();

    public abstract void processEvent(AB2Event event);

}
