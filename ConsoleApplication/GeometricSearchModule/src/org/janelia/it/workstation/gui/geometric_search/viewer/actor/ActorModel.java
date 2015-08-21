package org.janelia.it.workstation.gui.geometric_search.viewer.actor;

import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerEventListener;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.ActorAddedEvent;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.EventManager;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.RenderableAddedEvent;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.VoxelViewerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by murphys on 8/7/2015.
 */
public class ActorModel implements VoxelViewerEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ActorModel.class);

    List<Actor> actors=new ArrayList<>();

    @Override
    public void processEvent(VoxelViewerEvent event) {
        logger.info("processEvent() with event type="+event.getClass().getName());
        if (event instanceof RenderableAddedEvent) {
            RenderableAddedEvent renderableAddedEvent = (RenderableAddedEvent)event;
            Actor actor=renderableAddedEvent.getRenderable().createAndSetActor();
            addActor(actor);
        }
    }

    private void addActor(Actor actor) {
        actors.add(actor);
        EventManager.sendEvent(this, new ActorAddedEvent(actor));
    }

}
