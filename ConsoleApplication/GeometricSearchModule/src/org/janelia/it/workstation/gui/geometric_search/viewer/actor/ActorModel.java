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
            try {
                logger.info("Check 9.0");
                SharedResourceNeededEvent sharedResourceNeededEvent = (SharedResourceNeededEvent) event;
                logger.info("Check 9.1");
                ActorSharedResource sharedResource = sharedResourceNeededEvent.getActorSharedResource();
                logger.info("Check 9.2");
                if (sharedResource == null) {
                    logger.info("Check 9.2 - sharedResource is null!");
                } else {
                    logger.info("Check 9.2 - sharedResource is not null");
                }
                attachedSharedResources.add(sharedResource);
                logger.info("Check 9.3");
                for (Actor actor : sharedResource.getSharedActorList()) {
                    logger.info("Check 9.4");
                    logger.info("Actor name=" + actor.getName());
                    EventManager.sendEvent(this, new ActorAddedEvent(actor));
                    logger.info("Check 9.5");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.error(ex.toString());
            }
        }
        else if (event instanceof SharedResourceNotNeededEvent) {
            logger.info("Check 10.0");
            try {
                SharedResourceNotNeededEvent notNeededEvent = (SharedResourceNotNeededEvent) event;
                String notNeededName = notNeededEvent.getResourceName();
                Set<ActorSharedResource> removeSet=new HashSet<>();
                for (ActorSharedResource sharedResource : attachedSharedResources) {
                    if (sharedResource.getName().equals(notNeededName)) {
                        logger.info("Check 10.1");
                        for (Actor actor : sharedResource.getSharedActorList()) {
                            logger.info("Check 10.2");
                            logger.info("Check 10.3 - actor name=" + actor.getName());
                            EventManager.sendEvent(this, new ActorRemovedEvent(actor));
                            logger.info("Check 10.4");
                        }
                        logger.info("Check 10.5");
                        removeSet.add(sharedResource);
                        logger.info("Check 10.6");
                    }
                }
                for (ActorSharedResource sharedResource : removeSet) {
                    attachedSharedResources.remove(sharedResource);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.error(ex.toString());
            }
            logger.info("Check 10.7");
        }
    }

    private void addActor(Actor actor) {
        actors.add(actor);
        EventManager.sendEvent(this, new ActorAddedEvent(actor));
    }

}
