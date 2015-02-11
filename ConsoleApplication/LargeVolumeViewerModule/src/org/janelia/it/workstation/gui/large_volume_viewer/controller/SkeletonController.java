/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import java.awt.Color;
import java.util.List;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Skeleton;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.SkeletonActor;
import org.janelia.it.workstation.tracing.AnchoredVoxelPath;

/**
 * This hands off interesting driving info to skeleton.
 * @author fosterl
 */
public class SkeletonController implements AnchoredVoxelPathListener, TmGeoAnnotationAnchorListener, NextParentListener, GlobalColorChangeListener {
    private Skeleton skeleton;
    private SkeletonActor actor;
    
    public SkeletonController(Skeleton skeleton, SkeletonActor actor) {
        this.skeleton = skeleton;
        this.actor = actor;
    }
    
    //---------------------------------IMPLEMENTS AnchoredVoxelPathListener
    @Override
    public void addAnchoredVoxelPath(AnchoredVoxelPath path) {
        skeleton.addTracedSegment(path);
    }

    @Override
    public void addAnchoredVoxelPaths(List<AnchoredVoxelPath> paths) {
        skeleton.addTracedSegments(paths);
    }

    @Override
    public void removeAnchoredVoxelPath(AnchoredVoxelPath path) {
        skeleton.removeTracedSegment(path);
    }

    //--------------------------------IMPLEMENTS TmGeoAnnotationAnchorListener
    @Override
    public void anchorAdded(TmGeoAnnotation anchor) {
        skeleton.addTmGeoAnchor(anchor);
    }

    @Override
    public void anchorsAdded(List<TmGeoAnnotation> anchors) {
        skeleton.addTmGeoAnchors(anchors);
    }

    @Override
    public void anchorDeleted(TmGeoAnnotation anchor) {
        skeleton.deleteTmGeoAnchor(anchor);
    }

    @Override
    public void anchorReparented(TmGeoAnnotation anchor) {
        skeleton.reparentTmGeoAnchor(anchor);
    }

    @Override
    public void anchorMovedBack(TmGeoAnnotation anchor) {
        skeleton.moveTmGeoAnchorBack(anchor);
    }

    @Override
    public void clearAnchors() {
        skeleton.clear();
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
	
}
