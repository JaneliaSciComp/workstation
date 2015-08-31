package org.janelia.it.workstation.gui.geometric_search.viewer.gui;

import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerEventListener;
import org.janelia.it.workstation.gui.geometric_search.viewer.actor.Actor;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.ActorAddedEvent;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.ActorsClearAllEvent;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.VoxelViewerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by murphys on 8/20/2015.
 */
public class ActorPanel extends ScrollableColorRowPanel implements VoxelViewerEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ActorPanel.class);

    @Override
    public void processEvent(VoxelViewerEvent event) {
        logger.info("processEvent() event type="+event.getClass().getName());
        if (event instanceof ActorAddedEvent) {
            ActorAddedEvent actorAddedEvent=(ActorAddedEvent)event;
            Actor actor=actorAddedEvent.getActor();
            addEntry(actor.getName());
        } else if (event instanceof ActorsClearAllEvent) {
            clear();
        }
    }

}
