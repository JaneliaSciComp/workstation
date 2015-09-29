package org.janelia.it.workstation.gui.geometric_search.viewer.actor;

import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerEventListener;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
            Actor actor=getActorByName(actorSetVisibleEvent.getName());
            if (actor!=null) {
                if (actor.getName().equals(actorSetVisibleEvent.getName())) {
                    boolean isVisible = actorSetVisibleEvent.isVisible();
                    boolean alreadyVisible = actor.isVisible();
                    if (isVisible != alreadyVisible) {
                        actor.setIsVisible(isVisible);
                        EventManager.sendEvent(this, new ActorModifiedEvent());
                    }
                }
            }
            for (ActorSharedResource sharedResource : attachedSharedResources) {
                for (Actor resourceActor : sharedResource.getSharedActorList()) {
                    if (resourceActor.getName().equals(actorSetVisibleEvent.getName())) {
                        boolean isVisible = actorSetVisibleEvent.isVisible();
                        boolean alreadyVisible = resourceActor.isVisible();
                        if (isVisible != alreadyVisible) {
                            resourceActor.setIsVisible(isVisible);
                            EventManager.sendEvent(this, new ActorModifiedEvent());
                        }
                    }
                }
            }
        } else if (event instanceof SharedResourceNeededEvent) {
            try {
                SharedResourceNeededEvent sharedResourceNeededEvent = (SharedResourceNeededEvent) event;
                ActorSharedResource sharedResource = sharedResourceNeededEvent.getActorSharedResource();
                attachedSharedResources.add(sharedResource);
                EventManager.setDisallowViewerRefresh(true);
                for (Actor actor : sharedResource.getSharedActorList()) {
                    EventManager.sendEvent(this, new ActorAddedEvent(actor));
                }
                EventManager.setDisallowViewerRefresh(false);
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.error(ex.toString());
            }
        }
        else if (event instanceof SharedResourceNotNeededEvent) {
            try {
                SharedResourceNotNeededEvent notNeededEvent = (SharedResourceNotNeededEvent) event;
                String notNeededName = notNeededEvent.getResourceName();
                Set<ActorSharedResource> removeSet=new HashSet<>();
                for (ActorSharedResource sharedResource : attachedSharedResources) {
                    if (sharedResource.getName().equals(notNeededName)) {
                        for (Actor actor : sharedResource.getSharedActorList()) {
                            EventManager.sendEvent(this, new ActorRemovedEvent(actor));
                        }
                        removeSet.add(sharedResource);
                    }
                }
                for (ActorSharedResource sharedResource : removeSet) {
                    attachedSharedResources.remove(sharedResource);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.error(ex.toString());
            }
        }
        else if (event instanceof ActorAOSEvent) {
            ActorAOSEvent actorAOSEvent=(ActorAOSEvent)event;
            logger.info("Received AOS event, name="+actorAOSEvent.getActorName()+" type="+actorAOSEvent.getAosType());
        }
    }

    private void addActor(Actor actor) {
        if (getActorByName(actor.getName()) == null) {
            actors.add(actor);
            EventManager.sendEvent(this, new ActorAddedEvent(actor));
        } else {
            logger.error("Actor with duplicate name not permitted in ActorModel");
        }
    }

    private Actor getActorByName(String name) {
        for (Actor actor : actors) {
            if (actor.getName().equals(name)) {
                return actor;
            }
        }
        return null;
    }

}
