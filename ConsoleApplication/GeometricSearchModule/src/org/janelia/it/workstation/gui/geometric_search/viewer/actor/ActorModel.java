package org.janelia.it.workstation.gui.geometric_search.viewer.actor;

import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerEventListener;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.*;
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
    List<ActorSharedResource> attachedSharedResources=new ArrayList<>();

    @Override
    public void processEvent(VoxelViewerEvent event) {
        logger.info("processEvent() with event type="+event.getClass().getName());
        if (event instanceof RenderableAddedEvent) {
            RenderableAddedEvent renderableAddedEvent = (RenderableAddedEvent)event;
            Actor actor=renderableAddedEvent.getRenderable().createAndSetActor();
            addActor(actor);
        } else if (event instanceof RenderablesClearAllEvent) {
            actors.clear();
            attachedSharedResources.clear();
            EventManager.sendEvent(this, new ActorsClearAllEvent());
        } else if (event instanceof ActorSetVisibleEvent) {
            ActorSetVisibleEvent actorSetVisibleEvent=(ActorSetVisibleEvent)event;
            for (Actor actor : actors) {
                if (actor.getName().equals(actorSetVisibleEvent.getName())) {
                    boolean isVisible=actorSetVisibleEvent.isVisible();
                    boolean alreadyVisible=actor.isVisible();
                    if (isVisible!=alreadyVisible) {
                        actor.setIsVisible(isVisible);
                        EventManager.sendEvent(this, new ActorModifiedEvent());
                    }
                }
            }
            for (ActorSharedResource sharedResource : attachedSharedResources) {
                for (Actor actor : sharedResource.getSharedActorList()) {
                    if (actor.getName().equals(actorSetVisibleEvent.getName())) {
                        boolean isVisible = actorSetVisibleEvent.isVisible();
                        boolean alreadyVisible = actor.isVisible();
                        if (isVisible != alreadyVisible) {
                            actor.setIsVisible(isVisible);
                            EventManager.sendEvent(this, new ActorModifiedEvent());
                        }
                    }
                }
            }
        } else if (event instanceof SharedResourceNeededEvent) {
            SharedResourceNeededEvent sharedResourceNeededEvent=(SharedResourceNeededEvent)event;
            ActorSharedResource sharedResource=sharedResourceNeededEvent.getActorSharedResource();
            boolean alreadyShared=false;
            for (ActorSharedResource s2 : attachedSharedResources) {
                if (s2.getName().equals(sharedResource.getName())) {
                    alreadyShared=true;
                }
            }
            if (!alreadyShared) {
                attachedSharedResources.add(sharedResource);
                for (Actor actor : sharedResource.getSharedActorList()) {
                    EventManager.sendEvent(this, new ActorAddedEvent(actor));
                }
            }
        } else if (event instanceof SharedResourceNotNeededEvent) {
            SharedResourceNotNeededEvent notNeededEvent=(SharedResourceNotNeededEvent)event;
            String notNeededName=notNeededEvent.getResourceName();
            for (ActorSharedResource sharedResource : attachedSharedResources) {
                if (sharedResource.getName().equals(notNeededName)) {
                    for (Actor actor : sharedResource.getSharedActorList()) {
                        EventManager.sendEvent(this, new ActorRemovedEvent(actor));
                    }
                    attachedSharedResources.remove(sharedResource);
                }
            }
        }
    }

    private void addActor(Actor actor) {
        actors.add(actor);
        EventManager.sendEvent(this, new ActorAddedEvent(actor));
    }

}
