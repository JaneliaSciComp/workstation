package org.janelia.it.workstation.gui.slice_viewer.annotation;


// workstation imports

import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.geom.ParametrizedLine;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;

import org.janelia.it.workstation.signal.Signal1;
import org.janelia.it.workstation.signal.Slot1;

import org.janelia.it.workstation.api.entity_model.management.ModelMgr;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class AnnotationModel
/*

this class is responsible for handling requests from the AnnotationManager.  those
requests are ready-to-execute; the AnnotationManager has taken care of validation.
the requests may map one-to-one onto atomic operations in the TiledMicroscopeDAO,
or they may package up several of those actions.

public methods in this class that throw exceptions are the ones that involve db calls, and
they should all be called from worker threads.  the others, typically getters of
various info, do not.  private methods don't necessarily follow that pattern.

a note on entities: in general, we want to only work with the domain objects in 
this class.  use "updateCurrentWorkspace/Neuron" to keep the local copies of
those objects in sync with the back end.  other components will limit themselves
to accessing those two objects in general.

this class does not interact directly with the UI.  its slots are connected to
UI elements that select, and its signals are connected with a variety of UI elements
that need to respond to changing data.
*/
{
    private ModelMgr modelMgr;
    private SessionMgr sessionMgr;


    private TmWorkspace currentWorkspace;
    private TmNeuron currentNeuron;

    // ----- signals
    public Signal1<TmWorkspace> workspaceLoadedSignal = new Signal1<TmWorkspace>();

    public Signal1<TmNeuron> neuronSelectedSignal = new Signal1<TmNeuron>();

    public Signal1<TmGeoAnnotation> annotationAddedSignal = new Signal1<TmGeoAnnotation>();
    public Signal1<List<TmGeoAnnotation>> annotationsDeletedSignal = new Signal1<List<TmGeoAnnotation>>();
    public Signal1<TmGeoAnnotation> annotationReparentedSignal = new Signal1<TmGeoAnnotation>();
    public Signal1<TmGeoAnnotation> annotationNotMovedSignal = new Signal1<TmGeoAnnotation>();

    public Signal1<TmAnchoredPath> anchoredPathAddedSignal = new Signal1<TmAnchoredPath>();
    public Signal1<List<TmAnchoredPath>> anchoredPathsRemovedSignal = new Signal1<List<TmAnchoredPath>>();

    public Signal1<Long> pathTraceRequestedSignal = new Signal1<Long>();

    public Signal1<Color> globalAnnotationColorChangedSignal = new Signal1<Color>();

    // ----- slots
    public Slot1<TmNeuron> neuronClickedSlot = new Slot1<TmNeuron>() {
        @Override
        public void execute(TmNeuron neuron) {
            setCurrentNeuron(neuron);
        }
    };

    // ----- constants
    // name of entity that holds our workspaces
    public static final String WORKSPACES_FOLDER_NAME = "Workspaces";

    // how far away to try to put split anchors (pixels)
    private static final Double SPLIT_ANCHOR_DISTANCE = 60.0;


    public AnnotationModel() {
        modelMgr = ModelMgr.getModelMgr();
        sessionMgr = SessionMgr.getSessionMgr();

    }

    // current workspace methods
    public TmWorkspace getCurrentWorkspace() {
        return currentWorkspace;
    }

    private void updateCurrentWorkspace() {
        try {
            currentWorkspace = modelMgr.loadWorkspace(currentWorkspace.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadWorkspace(TmWorkspace workspace) {
        if (workspace != null) {
            currentWorkspace = workspace;
        } else {
            currentWorkspace = null;
        }
        workspaceLoadedSignal.emit(workspace);

        // clear current neuron
        setCurrentNeuron(null);
        neuronSelectedSignal.emit(null);

    }

    // preferences stuff
    public void setPreference(String key, String value) {
        if (currentWorkspace != null) {
            try {
                modelMgr.createOrUpdateWorkspacePreference(currentWorkspace.getId(),
                        key, value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // current neuron methods
    public TmNeuron getCurrentNeuron() {
        return currentNeuron;
    }

    public void setCurrentNeuron(TmNeuron neuron) {
        currentNeuron = neuron;
        updateCurrentNeuron();
        neuronSelectedSignal.emit(currentNeuron);
    }

    private void updateCurrentNeuron() {
        if (currentNeuron != null) {
            try {
                currentNeuron = modelMgr.loadNeuron(currentNeuron.getId());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // convenience methods
    /**
     * given the ID of an annotation, return an object wrapping it (or null)
     */
    public TmGeoAnnotation getGeoAnnotationFromID(Long annotationID) {
        if (getCurrentWorkspace() == null) {
            return null;
        }

        TmNeuron foundNeuron = getNeuronFromAnnotationID(annotationID);
        if (foundNeuron != null) {
            return foundNeuron.getGeoAnnotationMap().get(annotationID);
        } else {
            return null;
        }
    }

    /**
     * given the ID of an annotation, return the neuron that contains it (or null) 
     */
    public TmNeuron getNeuronFromAnnotationID(Long annotationID) {
        if (getCurrentWorkspace() == null) {
            return null;
        }

        TmNeuron foundNeuron = null;
        for (TmNeuron neuron: getCurrentWorkspace().getNeuronList()) {
            if (neuron.getGeoAnnotationMap().containsKey(annotationID)) {
                foundNeuron = neuron;
                break;
            }
        }
        return foundNeuron;
    }

    /**
     * given an annotation, find its ultimate parent, which is the root of
     * its neurite
     */
    public TmGeoAnnotation getNeuriteRootAnnotation(TmGeoAnnotation annotation) {
        if (annotation == null) {
            return annotation;
        }
        TmGeoAnnotation current = annotation;
        TmGeoAnnotation parent = annotation.getParent();
        while (parent !=null) {
            current = parent;
            parent = current.getParent();
        }
        return current;
    }

    /**
     * find the annotation closest to the input location, exluding
     * the given annotation (input null to include all annotations)
     */
    public TmGeoAnnotation getClosestAnnotation(Vec3 location, TmGeoAnnotation excludedAnnotation) {

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        TmGeoAnnotation closest = null;
        // I hate magic numbers as much as the next programmer, but surely this
        //  is big enough, right?
        double closestSquaredDist = 1.0e99;

        double dx, dy, dz, dist;

        for (TmNeuron neuron: getCurrentWorkspace().getNeuronList()) {
            for (TmGeoAnnotation root: neuron.getRootAnnotations()) {
                for (TmGeoAnnotation ann: root.getSubTreeList()) {
                    if (excludedAnnotation.getId().equals(ann.getId())) {
                        continue;
                    }
                    dx = ann.getX() - x;
                    dy = ann.getY() - y;
                    dz = ann.getZ() - z;
                    dist = dx * dx + dy * dy + dz * dz;
                    if (dist < closestSquaredDist) {
                        closestSquaredDist = dist;
                        closest = ann;
                    }
                }
            }
        }
        return closest;
    }

    /**
     * create a neuron in the current workspace
     *
     * @param name = name of neuron
     * @throws Exception
     */
    public void createNeuron(String name) throws Exception {
        TmNeuron neuron = modelMgr.createTiledMicroscopeNeuron(getCurrentWorkspace().getId(), name);

        updateCurrentWorkspace();
        workspaceLoadedSignal.emit(getCurrentWorkspace());

        setCurrentNeuron(neuron);
    }

    /**
     * rename the given neuron
     */
    public void renameCurrentNeuron(String name) throws Exception {
        // rename
        Long currentNeuronID = getCurrentNeuron().getId();
        Entity neuronEntity = modelMgr.getEntityById(currentNeuronID);
        modelMgr.renameEntity(neuronEntity, name);

        // update & notify
        updateCurrentWorkspace();
        workspaceLoadedSignal.emit(getCurrentWorkspace());

        TmNeuron neuron = modelMgr.loadNeuron(currentNeuronID);
        setCurrentNeuron(neuron);
        neuronSelectedSignal.emit(getCurrentNeuron());

    }

    public void deleteCurrentNeuron() throws Exception {
        if (getCurrentNeuron() == null) {
            return;
        }

        // keep a copy so we know what visuals to remove:
        TmNeuron deletedNeuron = getCurrentNeuron();

        // delete
        modelMgr.deleteEntityTree(getCurrentNeuron().getId());

        // delete anchor signals
        ArrayList<TmGeoAnnotation> tempAnnotationList = new ArrayList<TmGeoAnnotation>(deletedNeuron.getGeoAnnotationMap().values());
        annotationsDeletedSignal.emit(tempAnnotationList);

        // delete path signals
        ArrayList<TmAnchoredPath> tempPathList = new ArrayList<TmAnchoredPath>(deletedNeuron.getAnchoredPathMap().values());
        anchoredPathsRemovedSignal.emit(tempPathList);

        updateCurrentWorkspace();
        workspaceLoadedSignal.emit(getCurrentWorkspace());

        setCurrentNeuron(null);
    }

    /**
     * @param parentEntity = parent folder in hierarchy
     * @param brainSampleID = tiled microscope sample ID
     * @param name = name of new workspace
     * @throws Exception
     */
    public void createWorkspace(Entity parentEntity, Long brainSampleID, String name) throws Exception {
        TmWorkspace workspace = modelMgr.createTiledMicroscopeWorkspace(parentEntity.getId(),
                brainSampleID, name, sessionMgr.getSubject().getKey());

        loadWorkspace(workspace);
    }

    /**
     * we save our workspaces in a pre-defined area; this method returns it, creating if needed
     *
     * @return workspace folder entity
     * @throws Exception
     */
    public Entity getOrCreateWorkspacesFolder() throws Exception {
        Entity workspaceRootEntity = modelMgr.getCommonRootEntityByName(WORKSPACES_FOLDER_NAME);
        if (workspaceRootEntity == null) {
            workspaceRootEntity = modelMgr.createCommonRoot(WORKSPACES_FOLDER_NAME);
        }
        return workspaceRootEntity;
    }

    /**
     * add a root annotation to the given neuron; this is an annotation without a parent
     *
     * @param neuron = neuron object
     * @param xyz = x, y, z location of new annotation
     * @throws Exception
     */
    public void addRootAnnotation(TmNeuron neuron, Vec3 xyz) throws Exception {
        // the null  in this call means "this is a root annotation" (would otherwise
        //  be the parent)
        TmGeoAnnotation annotation = modelMgr.addGeometricAnnotation(neuron.getId(),
            null, 0, xyz.x(), xyz.y(), xyz.z(), "");

        updateCurrentWorkspace();
        if (neuron.getId().equals(getCurrentNeuron().getId())) {
            updateCurrentNeuron();
        }

        neuronSelectedSignal.emit(getCurrentNeuron());
        annotationAddedSignal.emit(annotation);
    }

    /**
     * add a child annotation
     *
     * @param parentAnn = parent annotation object
     * @param xyz = location of new child annotation
     * @throws Exception
     */
    public void addChildAnnotation(TmGeoAnnotation parentAnn, Vec3 xyz) throws Exception {
        if (parentAnn == null) {
            return;
        }

        TmNeuron neuron = getNeuronFromAnnotationID(parentAnn.getId());
        TmGeoAnnotation annotation = modelMgr.addGeometricAnnotation(neuron.getId(),
            parentAnn.getId(), 0, xyz.x(), xyz.y(), xyz.z(), "");

        updateCurrentWorkspace();
        if (neuron.getId().equals(getCurrentNeuron().getId())) {
            updateCurrentNeuron();
        }

        // must come after workspace and neuron are updated:
        if (automatedTracingEnabled()) {
            pathTraceRequestedSignal.emit(annotation.getId());
        }

        neuronSelectedSignal.emit(getCurrentNeuron());
        annotationAddedSignal.emit(annotation);
    }

    /**
     * move an existing annotation to a new location
     *
     * @param annotationID = ID of annotation to move
     * @param location = new location
     * @throws Exception
     */
    public void moveAnnotation(Long annotationID, Vec3 location) throws Exception {
        TmGeoAnnotation annotation = getGeoAnnotationFromID(annotationID);
        try {
            modelMgr.updateGeometricAnnotation(annotation, annotation.getIndex(),
                location.getX(), location.getY(), location.getZ(),
                annotation.getComment());
        } catch (Exception e) {
            // error means not persisted; however, in the process of moving,
            //  the marker's already been moved, to give interactive feedback
            //  to the user; so in case of error, tell the view to update to current
            //  position (pre-move)
            // this is unfortunately untested, because I couldn't think of an
            //  easy way to simulate or force a failure!
            annotationNotMovedSignal.emit(getGeoAnnotationFromID(annotationID));
            throw e;
        }

        // find each connecting annotation; if there's a traced path to it,
        //  remove it:
        TmNeuron neuron = getNeuronFromAnnotationID(annotationID);
        TmGeoAnnotation parent = annotation.getParent();
        if (parent != null) {
            removeAnchoredPath(annotation, parent);
        }
        for (TmGeoAnnotation neighbor: annotation.getChildren()) {
            removeAnchoredPath(annotation, neighbor);
        }

        updateCurrentWorkspace();
        updateCurrentNeuron();

        // must come after workspace and neuron are updated:
        if (automatedTracingEnabled()) {
            // trace to parent, and each child to this parent:
            pathTraceRequestedSignal.emit(annotation.getId());
            for (TmGeoAnnotation child: annotation.getChildren()) {
                pathTraceRequestedSignal.emit(child.getId());
            }
        }

        // this triggers the updates in, eg, the neurite list
        if (getCurrentNeuron() != null) {
            if (neuron.getId().equals(getCurrentNeuron().getId())) {
                neuronSelectedSignal.emit(getCurrentNeuron());
            }
        }

    }

    /**
     * merge the neurite that has source Annotation into the neurite containing
     * targetAnnotation
     */
    public void mergeNeurite(Long sourceAnnotationID, Long targetAnnotationID) throws Exception {

        TmGeoAnnotation sourceAnnotation = getGeoAnnotationFromID(sourceAnnotationID);
        TmNeuron sourceNeuron = getNeuronFromAnnotationID(sourceAnnotationID);

        // reroot source neurite to source ann
        modelMgr.rerootNeurite(sourceNeuron, sourceAnnotation);

        // reload the things we just changed:
        updateCurrentWorkspace();
        sourceAnnotation = getGeoAnnotationFromID(sourceAnnotationID);
        sourceNeuron = getNeuronFromAnnotationID(sourceAnnotationID);

        // if source neurite not in same neuron as dest neruite: move it
        TmNeuron targetNeuron = getNeuronFromAnnotationID(targetAnnotationID);
        if (!sourceNeuron.getId().equals(targetNeuron.getId())) {
            modelMgr.moveNeurite(sourceAnnotation, targetNeuron);
        }

        // refresh domain objects that we've changed and will use again:
        updateCurrentWorkspace();
        sourceAnnotation = getGeoAnnotationFromID(sourceAnnotationID);
        targetNeuron = getNeuronFromAnnotationID(targetAnnotationID);


        // reparent all source annotation's children to dest ann; remove
        //  traced paths while we're here
        for (TmGeoAnnotation child: sourceAnnotation.getChildren()) {
            modelMgr.reparentGeometricAnnotation(child, targetAnnotationID, targetNeuron);
            removeAnchoredPath(child, sourceAnnotation);
        }

        // finally, delete source ann
        modelMgr.deleteGeometricAnnotation(sourceAnnotationID);

        // update objects *again*, last time:
        updateCurrentWorkspace();
        setCurrentNeuron(targetNeuron);

        // get fresh target annotation, and redraw its children; trigger
        //  traced paths
        TmGeoAnnotation targetAnnotation = getGeoAnnotationFromID(targetAnnotationID);
        for (TmGeoAnnotation child: targetAnnotation.getChildren()) {
            annotationReparentedSignal.emit(child);
            if (automatedTracingEnabled()) {
                pathTraceRequestedSignal.emit(child.getId());
            }
        }

        // undraw deleted annotation
        List<TmGeoAnnotation> deleteList = new ArrayList<TmGeoAnnotation>();
        deleteList.add(sourceAnnotation);
        annotationsDeletedSignal.emit(deleteList);

    }

    /**
     * this method deletes a link, which is defined as an annotation with
     * one parent and no more than one child (not a root, not a branch point)
     *
     * @param link = annotation object
     * @throws Exception
     */
    public void deleteLink(TmGeoAnnotation link) throws Exception {
        if (link == null) {
            return;
        }

        // check it's a link; trust no one; error or return?
        if (link.getParent() == null || link.getChildren().size() > 1) {
            return;
        }

        // check we can find it
        // again, not clear if this should be an exception or not?
        TmNeuron neuron = getNeuronFromAnnotationID(link.getId());
        if (neuron == null) {
            // should this be an error?  it's a sign that the annotation has already
            //  been deleted, or something else that shouldn't happen
            return;
        }

        // delete it; reparent its child (if any) to its parent
        TmGeoAnnotation parent = link.getParent();
        TmGeoAnnotation child = null;
        if (link.getChildren().size() == 1) {
            child = link.getChildren().get(0);
            modelMgr.reparentGeometricAnnotation(child, parent.getId(), neuron);

            // if segment to child had a trace, remove it
            removeAnchoredPath(link, child);
        }
        // delete the deleted annotation that is to be deleted:
        modelMgr.deleteGeometricAnnotation(link.getId());

        // if segment to parent had a trace, remove it
        removeAnchoredPath(link, parent);

        updateCurrentWorkspace();
        updateCurrentNeuron();

        // if we're tracing, retrace if there's a new connection
        if (automatedTracingEnabled() && child != null) {
            pathTraceRequestedSignal.emit(child.getId());
        }

        // notifications

        // need to delete an anchor (which does undraw it); but then
        //  need to redraw the neurite, because we need the link
        //  from the reparenting to appear
        List<TmGeoAnnotation> deleteList = new ArrayList<TmGeoAnnotation>(1);
        deleteList.add(link);
        annotationsDeletedSignal.emit(deleteList);

        if (child != null) {
            // at this point, the child object is stale (still has its old parent);
            // grab it again from the neuron
            neuron = getNeuronFromAnnotationID(child.getId());
            child = neuron.getGeoAnnotationMap().get(child.getId());
            annotationReparentedSignal.emit(child);
        }
    }

    /**
     * this method deletes an annotation and all of its children, and its
     * children's children, yea, unto every generation that liveth
     *
     * @param rootAnnotation = annotation to be deleted along with its descendents
     * @throws Exception
     */
    public void deleteSubTree(TmGeoAnnotation rootAnnotation) throws Exception {
        if (rootAnnotation == null) {
            return;
        }

        TmNeuron neuron = getNeuronFromAnnotationID(rootAnnotation.getId());
        if (neuron == null) {
            // should this be an error?  it's a sign that the annotation has already
            //  been deleted, or something else that shouldn't happen
            return;
        }

        // delete annotation
        // in DAO, delete method is pretty simplistic; it doesn't update parents; however,
        //  as we're deleting the whole tree, that doesn't matter for us
        // delete in child-first order

        // grab the parent of the root before the root disappears:
        TmGeoAnnotation rootParent = rootAnnotation.getParent();

        List<TmGeoAnnotation> deleteList = rootAnnotation.getSubTreeList();
        for (TmGeoAnnotation annotation: deleteList) {
            // for each annotation, delete any paths traced to its children;
            //  do before the deletion!
            for (TmGeoAnnotation child: annotation.getChildren()) {
                removeAnchoredPath(annotation, child);
            }

            modelMgr.deleteGeometricAnnotation(annotation.getId());
        }
        // for the root annotation, also delete any traced paths to the parent, if it exists
        if (rootParent != null) {
            removeAnchoredPath(rootAnnotation, rootParent);
        }


        updateCurrentWorkspace();
        updateCurrentNeuron();

        // notify the public; "neuronSelected" will update the neurite tree
        neuronSelectedSignal.emit(getCurrentNeuron());
        annotationsDeletedSignal.emit(deleteList);
    }

    /**
     * this method "splits" an annotation; it creates a new annotation on the line
     * between the input annotation and its parent, displaced a bit toward the parent;
     * if it's a root, it inserts in the direction of either its single child, or it's an error
     *
     * @param annotation = annotation to be split
     */
    public void splitAnnotation(TmGeoAnnotation annotation) throws Exception {
        if (annotation == null) {
            return;
        }

        TmNeuron neuron = getNeuronFromAnnotationID(annotation.getId());

        // ann1 is the child of ann2 in both cases; if reverse, place the new point
        //  near ann2 instead of ann1
        TmGeoAnnotation annotation1;
        TmGeoAnnotation annotation2;
        boolean reverse;

        if (annotation.getParent() == null) {
            // root case is special; if one child, split toward child; otherwise, error
            //  (with zero or many children, ambiguous where to put new annotation)
            if (annotation.getChildren().size() != 1) {
                throw new Exception("cannot split root annotation with zero or many children");
            }
            annotation1 = annotation.getChildren().get(0);
            annotation2 = annotation;
            reverse = true;
        } else {
            // regular point: split toward the parent
            annotation1 = annotation;
            annotation2 = annotation.getParent();
            reverse = false;
        }

        // heuristic: try for a default separation in pixels; we don't want to
        //  go bigger, but we may need to go smaller; if the two anchors are
        //  already really close together, don't go beyond the halfway point
        //  between them
        // (remember, our t=0 is the original point)
        ParametrizedLine pLine = new ParametrizedLine(
            new Vec3(annotation1.getX(), annotation1.getY(), annotation1.getZ()),
            new Vec3(annotation2.getX(), annotation2.getY(), annotation2.getZ())
            );
        Double t = pLine.parameterFromPathLength(SPLIT_ANCHOR_DISTANCE);
        if (t > 0.5) {
            t = 0.5;
        }
        if (reverse) {
            t = 1.0 - t;
        }
        Vec3 newPoint = pLine.getPoint(t);

        // create the new annotation, child of original parent; then
        //  reparent existing annotation to new annotation
        TmGeoAnnotation newAnnotation = modelMgr.addGeometricAnnotation(neuron.getId(),
                annotation2.getId(), 0, newPoint.x(), newPoint.y(), newPoint.z(), "");

        //refresh neuron, then again, for updates
        neuron = modelMgr.loadNeuron(neuron.getId());
        modelMgr.reparentGeometricAnnotation(annotation1, newAnnotation.getId(), neuron);
        neuron = modelMgr.loadNeuron(neuron.getId());

        // if that segment had a trace, remove it
        removeAnchoredPath(annotation1, annotation2);


        // updates and signals:
        updateCurrentWorkspace();
        if (neuron.getId().equals(getCurrentNeuron().getId())){
            updateCurrentNeuron();
        }

        // retrace
        if (automatedTracingEnabled()) {
            pathTraceRequestedSignal.emit(newAnnotation.getId());
            pathTraceRequestedSignal.emit(annotation1.getId());
        }

        annotationAddedSignal.emit(newAnnotation);

        annotation1 = neuron.getGeoAnnotationMap().get(annotation1.getId());
        annotationReparentedSignal.emit(annotation1);
    }

    /**
     * reroot a neurite at the input annotation; it becomes the top level parent, and
     * other annotations' relations are adjusted to compensate
     *
     * @param newRootID = ID of new root annotation for neurite
     * @throws Exception
     */
    public void rerootNeurite(Long newRootID) throws Exception {
        // do it in the DAO layer
        TmGeoAnnotation newRoot = getGeoAnnotationFromID(newRootID);
        TmNeuron neuron = getNeuronFromAnnotationID(newRoot.getId());
        modelMgr.rerootNeurite(neuron, newRoot);

        // notify, etc.; don't need to redraw anything, but the neurite list etc. need to be reloaded
        updateCurrentWorkspace();
        if (neuron.getId().equals(getCurrentNeuron().getId())){
            updateCurrentNeuron();
            neuronSelectedSignal.emit(getCurrentNeuron());
        }
    }

    /**
     * split a neurite at the input node; the node is detached from its parent, and it and
     * its children become a new neurite in the same neuron
     *
     * @param newRootID = ID of root of new neurite
     * @throws Exception
     */
    public void splitNeurite(Long newRootID) throws Exception {
        TmGeoAnnotation newRoot = getGeoAnnotationFromID(newRootID);
        TmGeoAnnotation newRootParent = newRoot.getParent();
        removeAnchoredPath(newRoot, newRootParent);
        TmNeuron neuron = getNeuronFromAnnotationID(newRoot.getId());
        modelMgr.splitNeurite(neuron, newRoot);

        // update and notify
        updateCurrentWorkspace();
        updateCurrentNeuron();

        // the anchor at the split is essentially reparented to none
        annotationReparentedSignal.emit(getCurrentNeuron().getGeoAnnotationMap().get(newRootID));
        neuronSelectedSignal.emit(getCurrentNeuron());

    }

    public void addAnchoredPath(TmAnchoredPathEndpoints endpoints, List<List<Integer>> points) throws Exception{

        // check we can find both endpoints in same neuron
        //  don't need to check that they are neighboring; UI gesture already enforces it
        TmNeuron neuron1 = getNeuronFromAnnotationID(endpoints.getAnnotationID1());
        TmNeuron neuron2 = getNeuronFromAnnotationID(endpoints.getAnnotationID2());
        if (neuron1 == null || neuron2 == null) {
            // something's been deleted
            return;
        }
        if (!neuron1.getId().equals(neuron2.getId())) {
            throw new Exception("anchored path annotations are in different neurons");
        }


        // now verify that endpoints for path are still where they were when path
        //  was being drawn (ie, make sure user didn't move the endpoints in the meantime)
        // check that the first and last points in the list match the current locations of the
        //  annotations, in some order (despite stated convention, I have not found the point
        //  list to be in consistant order vis a vis the ordering of the annotation IDs)

        TmGeoAnnotation ann1 = neuron1.getGeoAnnotationMap().get(endpoints.getAnnotationID1());
        TmGeoAnnotation ann2 = neuron2.getGeoAnnotationMap().get(endpoints.getAnnotationID2());

        boolean order1 = annotationAtPoint(ann1, points.get(0)) && annotationAtPoint(ann2, points.get(points.size() - 1));
        boolean order2 = annotationAtPoint(ann2, points.get(0)) && annotationAtPoint(ann1, points.get(points.size() - 1));
        if (!order1 && !order2) {
            // something's been moved
            return;
        }


        // if a path between those endpoints exists, remove it first:
        if (neuron1.getAnchoredPathMap().containsKey(endpoints)) {
            removeAnchoredPath(neuron1.getAnchoredPathMap().get(endpoints));
        }

        // transform point list and persist
        TmAnchoredPath path = modelMgr.addAnchoredPath(neuron1.getId(), endpoints.getAnnotationID1(),
                endpoints.getAnnotationID2(), points);


        // updates
        updateCurrentWorkspace();
        if (neuron1.getId().equals(currentNeuron.getId())) {
            updateCurrentNeuron();
        }

        // send notification
        anchoredPathAddedSignal.emit(path);

    }

    /**
     * used in addAnchoredPath; is the annotation's location, truncated to integer, the
     * same as the list of integers?
     */
    private boolean annotationAtPoint(TmGeoAnnotation annotation, List<Integer> pointList) {
        return annotation.getX().intValue() == pointList.get(0) &&
                annotation.getY().intValue() == pointList.get(1) &&
                annotation.getZ().intValue() == pointList.get(2);
    }

    /**
     * remove an anchored path between two annotations, if one exists;
     * should only be called within AnnotationModel, and it does not
     * update neurons or do any other cleanup
     */
    private void removeAnchoredPath(TmAnchoredPath path) throws  Exception {
        modelMgr.deleteAnchoredPath(path.getId());

        ArrayList<TmAnchoredPath> pathList = new ArrayList<TmAnchoredPath>();
        pathList.add(path);

        anchoredPathsRemovedSignal.emit(pathList);
    }

    /**
     * remove an anchored path between two annotations, if one exists;
     * should only be called within AnnotationModel, and it does not
     * update neurons or do any other cleanup
     */
    private void removeAnchoredPath(TmGeoAnnotation annotation1, TmGeoAnnotation annotation2)
        throws Exception {
        if (annotation1 == null || annotation2 == null) {
            return;
        }

        // we assume second annotation is in same neuron; if it's not, there's no path
        //  to remove anyway
        TmNeuron neuron1 = getNeuronFromAnnotationID(annotation1.getId());
        TmAnchoredPathEndpoints endpoints = new TmAnchoredPathEndpoints(annotation1.getId(),
                annotation2.getId());
        if (neuron1.getAnchoredPathMap().containsKey(endpoints)) {
            removeAnchoredPath(neuron1.getAnchoredPathMap().get(endpoints));
        }
    }

    public void setGlobalAnnotationColor(Color color) {
        setPreference(AnnotationsConstants.PREF_ANNOTATION_COLOR_GLOBAL,
            String.format("%d:%d:%d:%d", color.getRed(), color.getGreen(),
                color.getBlue(), color.getAlpha()));

        // persisted, so go ahead and change it
        globalAnnotationColorChangedSignal.emit(color);
    }

    private boolean automatedTracingEnabled() {
        String automaticTracingPref = getCurrentWorkspace().getPreferences().getProperty(AnnotationsConstants.PREF_AUTOMATIC_TRACING);
        if (automaticTracingPref != null) {
            return Boolean.parseBoolean(automaticTracingPref);
        } else {
            return false;
        }
    }
}
