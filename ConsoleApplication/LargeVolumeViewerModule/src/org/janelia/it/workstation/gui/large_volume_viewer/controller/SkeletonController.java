/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import java.util.ArrayList;
import java.util.List;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.LargeVolumeViewerTranslator;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Anchor;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Skeleton;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Skeleton.AnchorSeed;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.SkeletonActor;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyle;
import org.janelia.it.workstation.tracing.AnchoredVoxelPath;

/**
 * This hands off interesting driving info to skeleton.
 * @author fosterl
 */
public class SkeletonController implements AnchoredVoxelPathListener, TmGeoAnnotationAnchorListener,
        NextParentListener, NeuronStyleChangeListener {
    private Skeleton skeleton;
    private List<SkeletonActor> actors = new ArrayList<>();
    private SkeletonAnchorListener skeletonAnchorListener;
    private AnnotationManager annoMgr;
    private LargeVolumeViewerTranslator lvvTranslator;
    private QuadViewController qvController;
    
    private static SkeletonController instance = new SkeletonController();
    
    private long nextParentId = -1;
    
    private SkeletonController() {
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
        lvvTranslator = null;
        qvController = null;
    }

    public void registerForEvents(SkeletonActor actor) {
        this.actors.add(actor);
        actor.setNextParentByID(nextParentId);
    }
    
    public void registerForEvents(LargeVolumeViewerTranslator lvvTranslator) {
        this.lvvTranslator = lvvTranslator;
    }
    
    public void registerForEvents(QuadViewController qvController) {
        this.qvController = qvController;
    }

    //---------------------------------IMPLEMENTS AnchoredVoxelPathListener
    @Override
    public void addAnchoredVoxelPath(AnchoredVoxelPath path) {
        skeleton.addTracedSegment(path);
        skeletonChanged();
    }

    @Override
    public void addAnchoredVoxelPaths(List<AnchoredVoxelPath> paths) {
        skeleton.addTracedSegments(paths);
        skeletonChanged();
    }

    @Override
    public void removeAnchoredVoxelPath(AnchoredVoxelPath path) {
        skeleton.removeTracedSegment(path);
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
            actor.clearStyles();
        }
		skeletonChanged();
    }

    //--------------------------------IMPLEMENTS NextParentListener    
    @Override
    public void setNextParent(Long id) {
        nextParentId = id;
        for (SkeletonActor actor: actors) {
            actor.setNextParentByID(id);
        }
    }

    @Override
    public void neuronStyleChanged(TmNeuron neuron, NeuronStyle style) {
        for (SkeletonActor actor: actors) {
            actor.changeNeuronStyle(neuron, style);
        }
    }

    public void annotationSelected( Long guid ) {
        lvvTranslator.annotationSelected(guid);
    }
    
    public void skeletonChanged() {
        for (SkeletonActor actor: actors) {
            actor.updateAnchors();
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
    
    private class ControllerSkeletonAnchorListener implements SkeletonAnchorListener {

        @Override
        public void anchorMoved(Anchor anchor) {
            annoMgr.moveAnchor(anchor);
            skeletonChanged();
        }
        
        @Override
        public void anchorMovedSilent(Anchor anchor) {
            skeletonChanged();
        }
        
    }
	
}
