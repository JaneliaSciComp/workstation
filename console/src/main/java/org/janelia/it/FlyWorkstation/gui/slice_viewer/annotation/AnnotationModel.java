package org.janelia.it.FlyWorkstation.gui.slice_viewer.annotation;


// workstation imports

import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.geom.ParametrizedLine;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;

import org.janelia.it.FlyWorkstation.signal.Signal1;
import org.janelia.it.FlyWorkstation.signal.Slot1;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;

import java.util.ArrayList;
import java.util.List;


public class AnnotationModel
{
    private ModelMgr modelMgr;
    private SessionMgr sessionMgr;


    private TmWorkspace currentWorkspace;
    private TmNeuron currentNeuron;

    // signals & slots
    public Signal1<TmWorkspace> workspaceLoadedSignal = new Signal1<TmWorkspace>();
    public Signal1<TmNeuron> neuronSelectedSignal = new Signal1<TmNeuron>();

    public Signal1<TmGeoAnnotation> anchorAddedSignal = new Signal1<TmGeoAnnotation>();
    public Signal1<List<TmGeoAnnotation>> anchorsDeletedSignal = new Signal1<List<TmGeoAnnotation>>();
    public Signal1<TmGeoAnnotation> anchorReparentedSignal = new Signal1<TmGeoAnnotation>();

    // move or change, eg, comment:
    public Signal1<TmGeoAnnotation> anchorUpdatedSignal = new Signal1<TmGeoAnnotation>();


    public Slot1<TmNeuron> neuronClickedSlot = new Slot1<TmNeuron>() {
        @Override
        public void execute(TmNeuron neuron) {
            setCurrentNeuron(neuron);
        }
    };

    // constants
    public static final String WORKSPACES_FOLDER_NAME = "Workspaces";

    // how far away to try to put split anchors (pixels)
    private static final Double SPLIT_ANCHOR_DISTANCE = 60.0;


    public AnnotationModel() {
        // set up
        modelMgr = ModelMgr.getModelMgr();
        sessionMgr = SessionMgr.getSessionMgr();

    }

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

    public TmGeoAnnotation getGeoAnnotationFromID(Long annotationID) {
        TmWorkspace workspace = getCurrentWorkspace();
        if (workspace == null) {
            return null;
        }

        TmGeoAnnotation foundAnnotation = null;
        for (TmNeuron neuron: workspace.getNeuronList()) {
            if (neuron.getGeoAnnotationMap().containsKey(annotationID)) {
                foundAnnotation = neuron.getGeoAnnotationMap().get(annotationID);
                break;
            }
        }
        return foundAnnotation;
    }

    public TmNeuron getNeuronFromAnnotation(Long annotationID) {
        TmWorkspace workspace = getCurrentWorkspace();
        if (workspace == null) {
            return null;
        }

        TmNeuron foundNeuron = null;
        for (TmNeuron neuron: workspace.getNeuronList()) {
            if (neuron.getGeoAnnotationMap().containsKey(annotationID)) {
                foundNeuron = neuron;
                break;
            }
        }
        return foundNeuron;

    }

    public void createNeuron(String name) throws Exception {

        TmNeuron neuron = modelMgr.createTiledMicroscopeNeuron(getCurrentWorkspace().getId(), name);

        updateCurrentWorkspace();
        workspaceLoadedSignal.emit(currentWorkspace);

        setCurrentNeuron(neuron);
    }

    public void createWorkspace(Entity parentEntity, Long brainSampleID, String name) throws Exception {
        TmWorkspace workspace = modelMgr.createTiledMicroscopeWorkspace(parentEntity.getId(),
            brainSampleID, name, sessionMgr.getSubject().getKey());

        // trigger workspace load (no updates; this method takes care of it all)
        loadWorkspace(workspace);
    }

    public Entity getOrCreateWorkspacesFolder() throws Exception {
        Entity workspaceRootEntity = modelMgr.getCommonRootEntityByName(WORKSPACES_FOLDER_NAME);
        if (workspaceRootEntity == null) {
            workspaceRootEntity = modelMgr.createCommonRoot(WORKSPACES_FOLDER_NAME);
        }
        return workspaceRootEntity;
    }

    public void addRootAnnotation(TmWorkspace workspace, TmNeuron neuron, Vec3 xyz) throws Exception {
        // should assume current workspace and neuron?  don't need workspace at
        //  all, since neuron knows its workspace

        // the null means "this is a root annotation" (would be the parent)
        TmGeoAnnotation annotation = modelMgr.addGeometricAnnotation(neuron.getId(),
            null, 0, xyz.x(), xyz.y(), xyz.z(), "");

        // update
        updateCurrentWorkspace();
        if (neuron.getId().equals(currentNeuron.getId())) {
            updateCurrentNeuron();
        }

        // notify interested parties
        neuronSelectedSignal.emit(getCurrentNeuron());
        anchorAddedSignal.emit(annotation);

    }

    public void addChildAnnotation(TmNeuron neuron, TmGeoAnnotation parentAnn, Vec3 xyz) throws Exception {
        // should assume current neuron?  does parentAnn know its neuron anyway?

        // check parent ann?
        if (parentAnn == null) {
            return;
        }

        // create it
        TmGeoAnnotation annotation = modelMgr.addGeometricAnnotation(neuron.getId(),
            parentAnn.getId(), 0, xyz.x(), xyz.y(), xyz.z(), "");

        // update
        updateCurrentWorkspace();
        if (neuron.getId().equals(currentNeuron.getId())) {
            updateCurrentNeuron();
        }

        // notify people
        neuronSelectedSignal.emit(getCurrentNeuron());
        anchorAddedSignal.emit(annotation);
    }

    public void moveAnnotation(Long annotationID, Vec3 location) throws Exception {
        TmGeoAnnotation annotation = getGeoAnnotationFromID(annotationID);
        try {
        modelMgr.updateGeometricAnnotation(annotation, annotation.getIndex(),
            location.getX(), location.getY(), location.getZ(),
            annotation.getComment());
        } catch (Exception e) {
            // error means not persisted; however, in the process of moving,
            //  the marker's been moved; tell the view to update to current
            //  position even after an error
            // this is unfortunately untested, because I couldn't think of an
            //  easy way to simulate or force a failure!
            anchorUpdatedSignal.emit(getGeoAnnotationFromID(annotationID));
            throw e;
        }

        // update
        updateCurrentWorkspace();
        updateCurrentNeuron();


        // notify
        anchorUpdatedSignal.emit(getGeoAnnotationFromID(annotationID));
    }

    /**
     * this method deletes a link, which is defined as an annotation with
     * one parent and no more than one child (not a root, not a branch point)
     *
     * @param link
     * @throws Exception
     */
    public void deleteLink(TmGeoAnnotation link) throws Exception {
        if (link == null) {
            return;
        }

        // check it's a link; trust no one
        // error or return?
        if (link.getParent() == null || link.getChildren().size() > 1) {
            return;
        }

        // check we can find it
        // again, not clear if this should be an exception or not?
        TmNeuron neuron = getNeuronFromAnnotation(link.getId());
        if (neuron == null) {
            // should this be an error?  it's a sign that the annotation has already
            //  been deleted, or something else that shouldn't happen
            return;
        }

        // delete it; reparent its child (if any) to its parent
        // so that means delete one annotation, update two others;
        //  actually, since geo ann doesn't store children, only need
        //  to update the one we reparent

        TmGeoAnnotation parent = link.getParent();
        TmGeoAnnotation child = null;
        if (link.getChildren().size() == 1) {
            child = link.getChildren().get(0);
            modelMgr.reparentGeometricAnnotation(child, parent.getId(), neuron);
        }
        // delete the deleted annotation that is to be deleted:
        modelMgr.deleteGeometricAnnotation(link.getId());

        // updates
        updateCurrentWorkspace();
        updateCurrentNeuron();


        // notifications

        // need to delete an anchor (which does undraw it); but then
        //  need to redraw the neurite, because we need the link
        //  from the reparenting to appear
        List<TmGeoAnnotation> deleteList = new ArrayList<TmGeoAnnotation>(1);
        deleteList.add(link);
        anchorsDeletedSignal.emit(deleteList);

        if (child != null) {
            // at this point, the child is stale (still has its old parent);
            //  it's ok in the db, but the object is stale; grab it again:
            // this works but I don't like it:
            neuron = getNeuronFromAnnotation(child.getId());
            child = neuron.getGeoAnnotationMap().get(child.getId());
            anchorReparentedSignal.emit(child);
        }

    }

    /**
     * this method deletes an annotation and all of its children, and its
     * children's children, yea, unto every generation that liveth
     *
     * @param rootAnnotation
     * @throws Exception
     */
    public void deleteSubTree(TmGeoAnnotation rootAnnotation) throws Exception {
        if (rootAnnotation == null) {
            return;
        }

        TmNeuron neuron = getNeuronFromAnnotation(rootAnnotation.getId());
        if (neuron == null) {
            // should this be an error?  it's a sign that the annotation has already
            //  been deleted, or something else that shouldn't happen
            return;
        }

        // delete annotation
        // in DAO, delete method is pretty simplistic; it doesn't update parents; however,
        //  as we're deleting the whole tree, that doesn't matter for us
        // delete in child-first order
        List<TmGeoAnnotation> deleteList = rootAnnotation.getSubTreeList();
        for (TmGeoAnnotation annotation: deleteList) {
            modelMgr.deleteGeometricAnnotation(annotation.getId());
        }

        // update
        updateCurrentWorkspace();
        updateCurrentNeuron();

        // notify the public
        // why this notification??
        neuronSelectedSignal.emit(getCurrentNeuron());
        anchorsDeletedSignal.emit(deleteList);
    }

    /**
     * this method "splits" an annotation; it creates a new annotation on the line
     * between the input annotation and its parent, displaced a bit toward the parent;
     * if it's a root, it inserts in the direction of either its single child, or it's an error
     *
     * @param annotation
     */
    public void splitAnnotation(TmGeoAnnotation annotation) throws Exception {
        if (annotation == null) {
            return;
        }

        TmNeuron neuron = getNeuronFromAnnotation(annotation.getId());

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
        // how to get the neuron to refresh here?
        modelMgr.invalidateCache(modelMgr.getEntityById(neuron.getId()), true);
        // retrieve *again*
        neuron = new TmNeuron(modelMgr.getEntityById(neuron.getId()));
        modelMgr.reparentGeometricAnnotation(annotation1, newAnnotation.getId(), neuron);

        // updates and signals:
        updateCurrentWorkspace();
        updateCurrentNeuron();

        anchorAddedSignal.emit(newAnnotation);

        // carefully refresh *again* (I really need to fix the underlying issue here...)
        modelMgr.invalidateCache(modelMgr.getEntityById(neuron.getId()), true);
        neuron = new TmNeuron(modelMgr.getEntityById(neuron.getId()));
        annotation1 = neuron.getGeoAnnotationMap().get(annotation1.getId());
        anchorReparentedSignal.emit(annotation1);

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

}
