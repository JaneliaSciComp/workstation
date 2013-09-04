package org.janelia.it.FlyWorkstation.gui.slice_viewer.annotation;


// workstation imports

import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;

import org.janelia.it.FlyWorkstation.signal.Signal1;
import org.janelia.it.FlyWorkstation.signal.Slot1;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;

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
        updateCurrentNeuron();

        // notify
        anchorUpdatedSignal.emit(getGeoAnnotationFromID(annotationID));
    }

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
