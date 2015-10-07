package org.janelia.it.workstation.gui.geometric_search.viewer.event;

import org.janelia.it.workstation.gui.geometric_search.viewer.actor.Actor;

/**
 * Created by murphys on 9/8/2015.
 */
public class ActorRemovedEvent extends VoxelViewerEvent {

    Actor actor;

    public ActorRemovedEvent(Actor actor ) {
        this.actor=actor;
    }

    public Actor getActor() {
        return actor;
    }
}
