package org.janelia.workstation.gui.large_volume_viewer.skeleton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JComponent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.workstation.controller.listener.TmGeoAnnotationAnchorListener;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.geom.Vec3;
import org.janelia.workstation.gui.large_volume_viewer.controller.QuadViewController;
import org.janelia.workstation.gui.large_volume_viewer.listener.*;
import org.janelia.workstation.gui.large_volume_viewer.controller.TraceMode;
import org.janelia.workstation.gui.large_volume_viewer.controller.AnnotationManager;
import org.janelia.workstation.gui.large_volume_viewer.controller.LargeVolumeViewerTranslator;
import org.janelia.workstation.gui.large_volume_viewer.skeleton.Skeleton.AnchorSeed;
import org.janelia.workstation.gui.viewer3d.mesh.actor.MeshDrawActor;
import org.janelia.workstation.gui.large_volume_viewer.tracing.AnchoredVoxelPath;
import org.janelia.model.domain.tiledMicroscope.AnnotationNavigationDirection;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This hands off interesting driving info to skeleton.
 * @author fosterl
 */
public class SkeletonController implements AnchoredVoxelPathListener, TmGeoAnnotationAnchorListener,
        NextParentListener {

    private static final Logger log = LoggerFactory.getLogger(SkeletonController.class);

    private Skeleton skeleton;
    private List<SkeletonActor> actors = new ArrayList<>();
    private List<JComponent> updateListeners = new ArrayList<>();
    private MeshDrawActor meshDrawActor;
    private SkeletonAnchorListener skeletonAnchorListener;
    private AnnotationManager annoMgr;
    private LargeVolumeViewerTranslator lvvTranslator;
    private QuadViewController qvController;
    private NVTTableModelListener nvtTableModelListener;
    private TableModel nvtTableModel;
    private Timer meshDrawUpdateTimer;
    private boolean skipSkeletonChange=false;

    private static SkeletonController instance = new SkeletonController();
    
    private Long nextParentId = -1L;
    
    private SkeletonController() {
    }
    
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
        lvvTranslator = null;
        qvController = null;
    }
    
    public void registerForEvents(SkeletonActor actor) {
        this.actors.add(actor);
        actor.getModel().setNextParentByID(nextParentId);
    }
    
    public void unregister(SkeletonActor actor) {
        this.actors.remove(actor);        
    }
    
    public void registerForEvents(LargeVolumeViewerTranslator lvvTranslator) {
        this.lvvTranslator = lvvTranslator;
    }
    
    public void registerForEvents(QuadViewController qvController) {
        this.qvController = qvController;
    }
    
    public void registerForEvents(MeshDrawActor meshDrawActor, TableModel tableModel) {
        if (nvtTableModelListener != null) {
            if (nvtTableModel != null)
            nvtTableModel.removeTableModelListener(nvtTableModelListener);
        }
        this.meshDrawActor = meshDrawActor;
        nvtTableModel = tableModel;
        nvtTableModel.addTableModelListener(new NVTTableModelListener());
        meshDrawActor.refresh();
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
    
    public void remoteAddAnchoredVoxelPaths(List<AnchoredVoxelPath> paths) {
        skeleton.addTracedSegments(paths);
        skeleton.incrementAnchorVersion();
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
     
    //--------------------------------IMPLEMENTS TmGeoAnnotationAnchorListener
    @Override
    public void anchorAdded(TmGeoAnnotation tmAnchor) {
        Anchor anchor = skeleton.addTmGeoAnchor(tmAnchor);
        anchor.setSkeletonAnchorListener(skeletonAnchorListener);
        skeletonChanged();
    }

    @Override
    public void anchorsAdded(List<TmGeoAnnotation> tmAnchors) {
        List<Anchor> anchors = skeleton.addTmGeoAnchors(tmAnchors);
        for (Anchor anchor: anchors) {
            anchor.setSkeletonAnchorListener(skeletonAnchorListener);
        }
        skeletonChanged();
    }
    
    public void remoteAnchorsAdded(List<TmGeoAnnotation> tmAnchors) {
        List<Anchor> anchors = skeleton.addTmGeoAnchors(tmAnchors);
        for (Anchor anchor: anchors) {
            anchor.setSkeletonAnchorListener(skeletonAnchorListener);
        }
    }

    @Override
    public void anchorDeleted(TmGeoAnnotation anchor) {
        skeleton.deleteTmGeoAnchor(anchor);
        skeletonChanged();
    }

    @Override
    public void anchorReparented(TmGeoAnnotation anchor) {
        skeleton.reparentTmGeoAnchor(anchor);
        skeletonChanged();
    }

    @Override
    public void anchorMovedBack(TmGeoAnnotation anchor) {
        skeleton.moveTmGeoAnchorBack(anchor);
    }

    @Override
    public void anchorMoved(TmGeoAnnotation anchor) {
        skeleton.moveTmGeoAnchor(anchor);
    }

    @Override
    public void clearAnchors(Collection<TmGeoAnnotation> annotations) {
        for(TmGeoAnnotation annotation : annotations) {
            Anchor anchor = skeleton.getAnchorByID(annotation.getId());
            if (anchor!=null) {
                skeleton.delete(anchor);
            }
        }
        skeletonChanged();
    }
    
    public void remoteClearAnchors(Collection<TmGeoAnnotation> annotations) {
        for(TmGeoAnnotation annotation : annotations) {
            Anchor anchor = skeleton.getAnchorByID(annotation.getId());
            if (anchor!=null) {
                skeleton.delete(anchor);
            }
        }
    }
    
    @Override
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

    public void annotationSelected( Long guid ) {
        lvvTranslator.annotationSelected(guid);
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

    private void updateMeshDrawActor() {
        if (meshDrawActor != null) {
            meshDrawActor.refresh();
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
                updateMeshDrawActor();
                fireComponentUpdate();
            }
        };
        meshDrawUpdateTimer.schedule(meshDrawUpdateTask, 10000);
    }

    @Override
    public void anchorRadiusChanged(TmGeoAnnotation anchor) {
        // Do nothing: radius has no effect on skeleton view
    }

    @Override
    public void anchorRadiiChanged(List<TmGeoAnnotation> anchorList) {
        // nothing: radius not indicated in skeleton view
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
	
    /**
     * This listener will update for changes made on the table of special
     * annotations.
     */
    private class NVTTableModelListener implements TableModelListener {
                
        @Override
        public void tableChanged(TableModelEvent e) {
            if (e.getColumn() > -1) {
                updateMeshDrawActor();
                fireComponentUpdate();
            }                    
        }

    }

}
