package org.janelia.it.workstation.gui.geometric_search.viewer.event;

import org.janelia.it.workstation.gui.geometric_search.viewer.actor.Actor;

/**
 * Created by murphys on 8/21/2015.
 */
public class ActorAddedEvent extends VoxelViewerEvent {

    Actor actor;

    public ActorAddedEvent(Actor actor) {
        this.actor=actor;
    }

    public Actor getActor() {
        return actor;
    }

    public void setActor(Actor actor) {
        this.actor = actor;
    }
}
