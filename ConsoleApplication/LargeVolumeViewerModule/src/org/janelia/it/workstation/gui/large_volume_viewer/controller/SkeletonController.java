/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import java.awt.Color;
import java.util.List;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Anchor;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Skeleton;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.SkeletonActor;
import org.janelia.it.workstation.tracing.AnchoredVoxelPath;

/**
 * This hands off interesting driving info to skeleton.
 * @author fosterl
 */
public class SkeletonController 
    implements AnchoredVoxelPathListener, TmGeoAnnotationAnchorListener, NextParentListener, GlobalColorChangeListener, SkeletonChangeListener {
    private Skeleton skeleton;
    private SkeletonActor actor;
    private SkeletonAnchorListener skeletonAnchorListener;
    private AnnotationManager annoMgr;
    
    public SkeletonController(Skeleton skeleton, SkeletonActor actor, AnnotationManager annoMgr) {
        this.skeleton = skeleton;
        this.annoMgr = annoMgr;
        this.actor = actor;
        skeleton.addSkeletonChangeListener(this);
        skeletonAnchorListener = new ControllerSkeletonAnchorListener();
        this.skeleton.setController(this);
    }

    //---------------------------------IMPLEMENTS AnchoredVoxelPathListener
    @Override
    public void addAnchoredVoxelPath(AnchoredVoxelPath path) {
        skeleton.addTracedSegment(path);
        skeleton.fireSkeletonChangeEvent();
    }

    @Override
    public void addAnchoredVoxelPaths(List<AnchoredVoxelPath> paths) {
        skeleton.addTracedSegments(paths);
        skeleton.fireSkeletonChangeEvent();
    }

    @Override
    public void removeAnchoredVoxelPath(AnchoredVoxelPath path) {
        skeleton.removeTracedSegment(path);
        skeleton.fireSkeletonChangeEvent();
    }

    //--------------------------------IMPLEMENTS TmGeoAnnotationAnchorListener
    @Override
    public void anchorAdded(TmGeoAnnotation tmAnchor) {
        Anchor anchor = skeleton.addTmGeoAnchor(tmAnchor);
        anchor.setSkeletonAnchorListener(skeletonAnchorListener);
        skeleton.fireSkeletonChangeEvent();
    }

    @Override
    public void anchorsAdded(List<TmGeoAnnotation> tmAnchors) {
        List<Anchor> anchors = skeleton.addTmGeoAnchors(tmAnchors);
        for (Anchor anchor: anchors) {
            anchor.setSkeletonAnchorListener(skeletonAnchorListener);
        }
        skeleton.fireSkeletonChangeEvent();
    }

    @Override
    public void anchorDeleted(TmGeoAnnotation anchor) {
        skeleton.deleteTmGeoAnchor(anchor);
        skeleton.fireSkeletonChangeEvent();
    }

    @Override
    public void anchorReparented(TmGeoAnnotation anchor) {
        skeleton.reparentTmGeoAnchor(anchor);
        skeleton.fireSkeletonChangeEvent();
    }

    @Override
    public void anchorMovedBack(TmGeoAnnotation anchor) {
        skeleton.moveTmGeoAnchorBack(anchor);
    }

    @Override
    public void clearAnchors() {
        skeleton.clear();
		skeleton.fireSkeletonChangeEvent();
    }

    //--------------------------------IMPLEMENTS NextParentListener    
    @Override
    public void setNextParent(Long id) {
        actor.setNextParentByID(id);
    }

    //--------------------------------IMPLEMENTS GlobalColorChangeListener    
    @Override
    public void globalAnnotationColorChanged(Color color) {
        actor.changeNeuronColor(color);
    }

    //--------------------------------IMPLEMENTS SkeletonChangeListener    
    @Override
    public void skeletonChanged() {
        actor.updateAnchors();
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
    
    private class ControllerSkeletonAnchorListener implements SkeletonAnchorListener {

        @Override
        public void anchorMoved(Anchor anchor) {
            annoMgr.moveAnchor(anchor);
            skeleton.fireSkeletonChangeEvent();
        }
        
        @Override
        public void anchorMovedSilent(Anchor anchor) {
            skeleton.fireSkeletonChangeEvent();
        }
        
    }
	
}
