package org.janelia.it.workstation.gui.geometric_search.viewer.event;

import org.janelia.it.workstation.gui.geometric_search.viewer.actor.ActorSharedResource;

/**
 * Created by murphys on 9/8/2015.
 */
public class SharedResourceNeededEvent extends VoxelViewerEvent {

    ActorSharedResource actorSharedResource;

    public SharedResourceNeededEvent(ActorSharedResource actorSharedResource) {
        this.actorSharedResource=actorSharedResource;
    }

    public ActorSharedResource getActorSharedResource() {
        return actorSharedResource;
    }

}
