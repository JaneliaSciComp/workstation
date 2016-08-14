/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import java.util.*;
import javax.swing.JComponent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.workstation.gui.large_volume_viewer.action.TraceMode;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.LargeVolumeViewerTranslator;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Anchor;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Skeleton;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Skeleton.AnchorSeed;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.SkeletonActor;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton_mesh.LineEnclosurePrecomputes;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyle;
import org.janelia.it.workstation.gui.viewer3d.mesh.actor.MeshDrawActor;
import org.janelia.it.workstation.tracing.AnchoredVoxelPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This hands off interesting driving info to skeleton.
 * @author fosterl
 */
public class SkeletonController implements AnchoredVoxelPathListener, TmGeoAnnotationAnchorListener,
        NextParentListener, NeuronStyleChangeListener {

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
        this.skipSkeletonChange=skipSkeletonChange;
    }

    public static SkeletonController getInstance() {
        return instance;
    }
    
    /** In a "driving" client that would normally construct, call this instead. */
    public void reestablish(Skeleton skeleton, AnnotationManager annotationMgr) {
        this.skeleton = skeleton;
        this.annoMgr = annotationMgr;
        skeletonAnchorListener = new ControllerSkeletonAnchorListener();
        this.skeleton.setController(this);

        actors.clear();
        LineEnclosurePrecomputes.clearWorkspaceRelevant();
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

    public Vec3 getAnnotationPosition( long annotationID ) {
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
    public void clearAnchors() {
        skeleton.clear();
        for (SkeletonActor actor: actors) {
            actor.getModel().clearStyles();
        }
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

    @Override
    public void neuronStyleChanged(TmNeuron neuron, NeuronStyle style) {
        for (SkeletonActor actor: actors) {
            actor.getModel().changeNeuronStyle(neuron, style);
        }
        refreshMeshDrawUpdateTimer();
        fireComponentUpdate();
    }

    @Override
    public void neuronStylesChanged(Map<TmNeuron, NeuronStyle> neuronStyleMap) {
        for (TmNeuron neuron: neuronStyleMap.keySet()) {
            for (SkeletonActor actor: actors) {
                actor.getModel().changeNeuronStyle(neuron, neuronStyleMap.get(neuron));
            }
        }
        refreshMeshDrawUpdateTimer();
        fireComponentUpdate();
    }

    public void annotationSelected( Long guid ) {
        lvvTranslator.annotationSelected(guid);
    }
    
    public void skeletonChanged() {
        if (!skipSkeletonChange) {
            for (SkeletonActor actor : actors) {
                actor.getModel().updateAnchors();
            }
            refreshMeshDrawUpdateTimer();
            fireComponentUpdate();
            log.info("anchor time=" + TraceMode.getTimerMs());
        }
    }

    public void deleteSubtreeRequested(Anchor anchor) {
        annoMgr.deleteSubtreeRequested(anchor);
    }

    public void splitAnchorRequested(Anchor anchor) {
        annoMgr.splitAnchorRequested(anchor);
    }

    public void rerootNeuriteRequested(Anchor anchor) {
        annoMgr.rerootNeuriteRequested(anchor);
    }

    public void splitNeuriteRequested(Anchor anchor) {
        annoMgr.splitNeuriteRequested(anchor);
    }

    public void deleteLinkRequested(Anchor anchor) {
        annoMgr.deleteLinkRequested(anchor);
    }

    public void addEditNoteRequested(Anchor anchor) {
        annoMgr.addEditNoteRequested(anchor);
    }

    public void moveNeuriteRequested(Anchor anchor) {
        annoMgr.moveNeuriteRequested(anchor);
    }

    public void changeNeuronStyleRequested(Anchor anchor) {
        annoMgr.chooseNeuronStyle(anchor);
    }

    public void setNeuronVisibilityRequested(Anchor anchor, boolean visibility) {
        annoMgr.setNeuronVisibility(anchor, visibility);
    }
    
    public void anchorAdded(AnchorSeed anchorSeed) {
        annoMgr.anchorAdded(anchorSeed);
    }
    
    public void pathTraceRequested(Long id) {
        qvController.pathTraceRequested(id);
    }

    public void navigationRelative(Long id, TmNeuron.AnnotationNavigationDirection direction) {
        Anchor anchor = skeleton.getAnchorByID(annoMgr.relativeAnnotation(id, direction));
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

    private class ControllerSkeletonAnchorListener implements SkeletonAnchorListener {

        @Override
        public void anchorMoved(Anchor anchor) {
            annoMgr.moveAnchor(anchor);
            skeleton.incrementAnchorVersion();
            skeletonChanged();
        }
        
        @Override
        public void anchorMovedSilent(Anchor anchor) {
            skeletonChanged();
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
