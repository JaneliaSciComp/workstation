package org.janelia.it.workstation.gui.geometric_search.viewer.actor;

import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GL4SimpleActor;

/**
 * Created by murphys on 8/6/2015.
 */
public abstract class Actor {

    String name;
    GL4SimpleActor glActor;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract GL4SimpleActor createAndSetGLActor();

    public void dispose() {}

}
