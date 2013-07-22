package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.annotation;


// workstation imports

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;

import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.Signal1;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.Slot1;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;

import java.util.List;


public class AnnotationModel
{
    private ModelMgr modelMgr;
    private SessionMgr sessionMgr;


    private Long currentWorkspaceID;
    private Long currentNeuronID;

    // signals & slots
    public Signal1<TmWorkspace> workspaceLoadedSignal = new Signal1<TmWorkspace>();
    public Signal1<TmNeuron> neuronSelectedSignal = new Signal1<TmNeuron>();

    public Signal1<TmGeoAnnotation> anchorAddedSignal = new Signal1<TmGeoAnnotation>();
    public Signal1<List<TmGeoAnnotation>> anchorsDeletedSignal = new Signal1<List<TmGeoAnnotation>>();

    public Slot1<TmNeuron> neuronClickedSlot = new Slot1<TmNeuron>() {
        @Override
        public void execute(TmNeuron neuron) {
            setCurrentNeuron(neuron);
        }
    };

    public TmWorkspace getCurrentWorkspace() {
        if (currentWorkspaceID != null) {
            try {
                return modelMgr.loadWorkspace(currentWorkspaceID);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    public TmNeuron getCurrentNeuron() {
        if (currentNeuronID != null) {
            try {
                return modelMgr.loadNeuron(currentNeuronID);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    private void setCurrentNeuron(TmNeuron neuron) {
        if (neuron != null) {
            currentNeuronID = neuron.getId();
        } else {
            currentNeuronID = null;
        }

        // refresh the neuron object!
        neuronSelectedSignal.emit(getCurrentNeuron());
    }

    public AnnotationModel() {
        // set up
        modelMgr = ModelMgr.getModelMgr();
        sessionMgr = SessionMgr.getSessionMgr();

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

    public boolean createNeuron(String name) {

        TmNeuron currentNeuron;
        try {
            currentNeuron = modelMgr.createTiledMicroscopeNeuron(getCurrentWorkspace().getId(), name);
            setCurrentNeuron(currentNeuron);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        // should eventually have an addNeuron signal and not reload entire workspace, but
        //  for now, keep it simple
        workspaceLoadedSignal.emit(getCurrentWorkspace());
        neuronSelectedSignal.emit(currentNeuron);

        return true;
        
    }

    public boolean createWorkspace(Entity parentEntity, Entity brainSample, String name) {
        TmWorkspace workspace;

        // do stuff
        Subject subject = sessionMgr.getSubject();

        try {
            workspace = modelMgr.createTiledMicroscopeWorkspace(parentEntity.getId(),
                brainSample.getId(), name, subject.getKey());
        }  catch (Exception e) {
            e.printStackTrace();
            return false;
        }



        // trigger workspace load
        loadWorkspace(workspace);


        return true;

    }

    public void addRootAnnotation(TmWorkspace workspace, TmNeuron neuron, Vec3 xyz) {
        // should assume current workspace and neuron?  don't need workspace at
        //  all, since neuron knows its workspace

        // does neuron already have a root annotation?  for now, we're restricted
        //  to one (will remove this restriction); basically, if it's got any, it's
        //  got a root
        if (neuron.getRootAnnotation() != null) {
            // I am uncomfortable doing nothing silently, but I currently have no
            //  way to return info upward
            return;
        }

        // the null means "this is a root annotation" (would be the parent)
        TmGeoAnnotation annotation;
        try {
            annotation = modelMgr.addGeometricAnnotation(neuron.getId(),
                null, 0, xyz.x(), xyz.y(), xyz.z(), " ");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // notify interested parties
        neuronSelectedSignal.emit(getCurrentNeuron());

        anchorAddedSignal.emit(annotation);

    }

    public void addChildAnnotation(TmNeuron neuron, TmGeoAnnotation parentAnn, Vec3 xyz) {
        // should assume current neuron?  does parentAnn know its neuron anyway?

        // check parent ann?
        if (parentAnn == null) {
            return;
        }

        // create it
        TmGeoAnnotation annotation;
        try {
            annotation = modelMgr.addGeometricAnnotation(neuron.getId(),
                    parentAnn.getId(), 0, xyz.x(), xyz.y(), xyz.z(), " ");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }


        // notify people
        neuronSelectedSignal.emit(getCurrentNeuron());

        anchorAddedSignal.emit(annotation);

    }


    public void deleteSubTree(TmGeoAnnotation rootAnnotation) {
        if (rootAnnotation == null) {
            return;
        }

        // delete annotation
        // in DAO, delete method is pretty simplistic; it doesn't update parents; however,
        //  as we're deleting the whole tree, that doesn't matter for us
        // delete in child-first order
        try {
            for (TmGeoAnnotation annotation: rootAnnotation.getSubTreeListReversed()) {
                modelMgr.deleteGeometricAnnotation(annotation.getId());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // notify the public
        neuronSelectedSignal.emit(getCurrentNeuron());
        
        anchorsDeletedSignal.emit(rootAnnotation.getSubTreeListReversed());

    }

    public void loadWorkspace(TmWorkspace workspace) {
        if (workspace != null) {
            currentWorkspaceID = workspace.getId();
        } else {
            currentWorkspaceID = null;
        }
        workspaceLoadedSignal.emit(workspace);

        // clear current neuron
        setCurrentNeuron(null);
        neuronSelectedSignal.emit(null);

    }

}
