package org.janelia.workstation.gui.large_volume_viewer.skeleton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JComponent;

import com.google.common.eventbus.Subscribe;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.workstation.controller.eventbus.*;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.geom.Vec3;
import org.janelia.workstation.gui.large_volume_viewer.controller.QuadViewController;
import org.janelia.workstation.gui.large_volume_viewer.listener.*;
import org.janelia.workstation.gui.large_volume_viewer.controller.TraceMode;
import org.janelia.workstation.gui.large_volume_viewer.controller.AnnotationManager;
import org.janelia.workstation.gui.large_volume_viewer.skeleton.Skeleton.AnchorSeed;
import org.janelia.workstation.gui.large_volume_viewer.tracing.AnchoredVoxelPath;
import org.janelia.model.domain.tiledMicroscope.AnnotationNavigationDirection;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This hands off interesting driving info to skeleton.
 * @author fosterl
 */
public class SkeletonController implements AnchoredVoxelPathListener, NextParentListener {

    private static final Logger log = LoggerFactory.getLogger(SkeletonController.class);

    private Skeleton skeleton;
    private List<SkeletonActor> actors = new ArrayList<>();
    private List<JComponent> updateListeners = new ArrayList<>();
    private SkeletonAnchorListener skeletonAnchorListener;
    private AnnotationManager annoMgr;
    private QuadViewController qvController;
    private Timer meshDrawUpdateTimer;
    private boolean skipSkeletonChange=false;

    private static SkeletonController instance = new SkeletonController();
    
    private Long nextParentId = -1L;
    
    private SkeletonController() {
        registerEvents();
    }

    private void registerEvents() {
        ViewerEventBus.registerForEvents(this);
    }

    // used for bulk updates... need a better mechanism so it doesn't feel so kludgy
    public void setSkipSkeletonChange(boolean skipSkeletonChange) {
        this.skipSkeletonChange = skipSkeletonChange;
    }

    public void beginTransaction() {
        this.skipSkeletonChange = true;
    }
    
    public void endTransaction() {
        this.skipSkeletonChange = false;
        skeletonChanged();
    }

    public static SkeletonController getInstance() {
        return instance;
    }
    
    public boolean editsAllowed() {
        return annoMgr.editsAllowed();
    }
    
    /** In a "driving" client that would normally construct, call this instead. */
    public void reestablish(Skeleton skeleton, AnnotationManager annotationMgr) {
        this.skeleton = skeleton;
        this.annoMgr = annotationMgr;
        skeletonAnchorListener = new ControllerSkeletonAnchorListener();
        this.skeleton.setController(this);

        actors.clear();
        qvController = null;
    }
    
    public void registerForEvents(SkeletonActor actor) {
        this.actors.add(actor);
        actor.getModel().setNextParentByID(nextParentId);
    }
    
    public void unregister(SkeletonActor actor) {
        this.actors.remove(actor);        
    }
    
    public void registerForEvents(QuadViewController qvController) {
        this.qvController = qvController;
    }
    
    public void registerForEvents(JComponent component) {
        updateListeners.add(component);
        // Retro-kick.
        this.fireComponentUpdate();
    }
    
    public void unregister(JComponent component) {
        if (updateListeners.contains(component)) {
            updateListeners.remove(component);
        }
    }

    public Vec3 getAnnotationPosition(long annotationID ) {
        final Anchor anchor = skeleton.getAnchorByID(annotationID);
        Vec3 location = null;
        if (anchor != null) {
            location = anchor.getLocation();
        }
        return location;
    }
    
    public void setLVVFocus( Vec3 focus ) {
        qvController.setCameraFocus(focus);
    }
    
    //---------------------------------IMPLEMENTS AnchoredVoxelPathListener
    @Override
    public void addAnchoredVoxelPath(AnchoredVoxelPath path) {
        skeleton.addTracedSegment(path);
        skeleton.incrementAnchorVersion();
        skeletonChanged();
    }

    @Override
    public void addAnchoredVoxelPaths(List<AnchoredVoxelPath> paths) {
        skeleton.addTracedSegments(paths);
        skeleton.incrementAnchorVersion();
        skeletonChanged();
    }

    @Override
    public void removeAnchoredVoxelPath(AnchoredVoxelPath path) {
        skeleton.removeTracedSegment(path);
        skeleton.incrementAnchorVersion();
        skeletonChanged();
    }

    @Override
    public void removeAnchoredVoxelPaths(Long neuronID) {
        skeleton.removeTracedSegments(neuronID);
        skeletonChanged();
    }
    
    public void remoteRemoveAnchoredVoxelPaths(Long neuronID) {
        skeleton.removeTracedSegments(neuronID);
    }

    @Subscribe
    public void anchorsAdded(AnnotationCreateEvent event) {
        Collection<TmGeoAnnotation> annotations = event.getAnnotations();
        processAnchorsAdded(annotations);
    }

    public void processAnchorsAdded (Collection<TmGeoAnnotation> vertexList) {
        List<Anchor> anchors = skeleton.addTmGeoAnchors(new ArrayList<>(vertexList));
        for (Anchor anchor: anchors) {
            anchor.setSkeletonAnchorListener(skeletonAnchorListener);
        }
        skeletonChanged();
    }

    @Subscribe
    public void anchorDeleted(AnnotationDeleteEvent event) {
        Collection<TmGeoAnnotation> annotations = event.getAnnotations();
        if (!annotations.isEmpty()) {
            for (TmGeoAnnotation annotation: annotations) {
                skeleton.deleteTmGeoAnchor(annotation);
            }
        }
        skeletonChanged();
    }

    @Subscribe
    public void anchorReparented(AnnotationParentReparentedEvent event) {
        Collection<TmGeoAnnotation> annotations = event.getAnnotations();
        if (!annotations.isEmpty()) {
            for (TmGeoAnnotation annotation: annotations) {
                skeleton.reparentTmGeoAnchor(annotation);
            }
        }
        skeletonChanged();
    }

    @Subscribe
    public void anchorMoved(AnnotationUpdateEvent event) {
        Collection<TmGeoAnnotation> annotations = event.getAnnotations();
        if (!annotations.isEmpty()) {
            for (TmGeoAnnotation annotation: annotations) {
                skeleton.moveTmGeoAnchor(annotation);
            }
        }
    }

    public void clearAnchors() {
        skeleton.clear();
		skeletonChanged();
    }

    //--------------------------------IMPLEMENTS NextParentListener
    @Override
    public void setNextParent(Long id) {
        nextParentId = id;
        for (SkeletonActor actor: actors) {
            actor.getModel().setNextParentByID(id);
        }
        fireComponentUpdate();
    }
    
    @Override
    public void setNextParent(Anchor parent) {
        for (SkeletonActor actor : actors) {
            actor.getModel().setNextParent(parent);
        }
        // Need not rebuild everything, each time the anchor is selected.
        //   ** this would make the whole display very slow ** updateMeshDrawActor();
        fireComponentUpdate();
    }

    @Subscribe
    public void annotationSelected(SelectionAnnotationEvent event) {
        List items = event.getItems();
        if (!items.isEmpty()) {
            TmGeoAnnotation selectedAnnotation = (TmGeoAnnotation)items.get(0);
            setNextParent(selectedAnnotation.getId());
        }
    }

    public void skeletonChanged() {
        skeletonChanged(false);
    }
    
    public void skeletonChanged(boolean forceUpdate) {
        if (!skipSkeletonChange) {
            for (SkeletonActor actor : actors) {
                if (forceUpdate) {
                    actor.getModel().forceUpdateAnchors();    
                }
                else {
                    actor.getModel().updateAnchors();
                }
            }
            refreshMeshDrawUpdateTimer();
            fireComponentUpdate();
            log.debug("anchor time=" + TraceMode.getTimerMs());
        }
    }

    public void moveNeuriteRequested(Anchor anchor) {
        annoMgr.moveNeuriteRequested(anchor);
    }
    
    public boolean checkOwnership(Long neuronID) {
        return TmModelManager.getInstance().checkOwnership(neuronID);
    }

    public void smartMergeNeuriteRequested(Anchor clickedAnchor, Anchor currentParentAnchor) {
        annoMgr.smartMergeNeuriteRequested(clickedAnchor, currentParentAnchor);
    }
    
    public void anchorAdded(AnchorSeed anchorSeed) {
        annoMgr.anchorAdded(anchorSeed);
    }
    
    public void pathTraceRequested(Long neuronId, Long annotationId) {
        qvController.pathTraceRequested(neuronId, annotationId);
    }

    public void navigationRelative(Long neuronId, Long annotationId, AnnotationNavigationDirection direction) {
        Anchor anchor = skeleton.getAnchorByID(annoMgr.relativeAnnotation(neuronId, annotationId, direction));
        setNextParent(anchor);
        qvController.setCameraFocus(anchor.getLocation());
    }

    public void fireComponentUpdate() {
        for (JComponent updateListener : updateListeners) {
            updateListener.validate();
            updateListener.repaint();
        }
    }

    /**
     * Some events, being very numerous, cause a deluge of requests, each
     * of which is time-consuming to honor.  Rather than honor each one,
     * a timer/delay mechanism is in place to allow a single refresh to
     * be carried out, after things become quiescent.
     */
    private void refreshMeshDrawUpdateTimer() {
        if (meshDrawUpdateTimer != null) {
            meshDrawUpdateTimer.cancel();
        }
        meshDrawUpdateTimer = new Timer();
        TimerTask meshDrawUpdateTask = new TimerTask() {
            @Override
            public void run() {
                fireComponentUpdate();
            }
        };
        meshDrawUpdateTimer.schedule(meshDrawUpdateTask, 10000);
    }

    private class ControllerSkeletonAnchorListener implements SkeletonAnchorListener {

        @Override
        public void anchorMoved(Anchor anchor) {
            annoMgr.moveAnchor(anchor);
            skeleton.incrementAnchorVersion();
            skeletonChanged();
        }
        
        @Override
        public void anchorMovedSilent(Anchor anchor) {
            skeletonChanged(true); // update display, but don't update all listeners
        }
        
    }

}
