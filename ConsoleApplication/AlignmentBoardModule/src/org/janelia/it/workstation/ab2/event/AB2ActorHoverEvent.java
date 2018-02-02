package org.janelia.it.workstation.ab2.event;

import org.janelia.it.workstation.ab2.gl.GLAbstractActor;

public class AB2ActorHoverEvent extends AB2Event {
    private GLAbstractActor hoverActor;

    public AB2ActorHoverEvent(GLAbstractActor hoverActor) {
        this.hoverActor=hoverActor;
    }

    public GLAbstractActor getHoverActor() {
        return hoverActor;
    }
}
