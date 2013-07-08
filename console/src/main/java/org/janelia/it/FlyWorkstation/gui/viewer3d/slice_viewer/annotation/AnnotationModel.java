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





public class AnnotationModel 
{
    private ModelMgr modelMgr;
    private SessionMgr sessionMgr;


    private Long currentWorkspaceID;
    private Long currentNeuronID;

    // signals & slots
    public Signal1<TmWorkspace> workspaceChangedSignal = new Signal1<TmWorkspace>();
    public Signal1<TmNeuron> neuronChangedSignal = new Signal1<TmNeuron>();

    public Slot1<TmNeuron> neuronSelectedSlot = new Slot1<TmNeuron>() {
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

    private void setCurrentWorkspace(TmWorkspace workspace) {
        if (workspace != null) {
            currentWorkspaceID = workspace.getId();
        } else {
            currentWorkspaceID = null;
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
        neuronChangedSignal.emit(neuron);
    }

    public AnnotationModel() {
        // set up
        modelMgr = ModelMgr.getModelMgr();
        sessionMgr = SessionMgr.getSessionMgr();

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

        // notify listeners
        neuronChangedSignal.emit(currentNeuron);

        // workspace info panel has neuron list, so it needs poking, too:
        workspaceChangedSignal.emit(getCurrentWorkspace());

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
            return;
        }

        // the null means "this is a root annotation" (would be the parent)
        try {
            TmGeoAnnotation ann = modelMgr.addGeometricAnnotation(neuron.getId(),
                null, 0, xyz.x(), xyz.y(), xyz.z(), "root annotation");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // notify interested parties


    }

    public void addChildAnnotation(TmNeuron neuron, TmGeoAnnotation parentAnn, Vec3 xyz) {
        // should assume current neuron?  does parentAnn know its neuron anyway?

        // check parent ann?

        // create it


        // notify people


    }

    public boolean exportSWC(String filename) {

        // probably will actually get more info passed in, eg, which neurite to export

        return false;

    }

    public void loadWorkspace(TmWorkspace workspace) {
        setCurrentWorkspace(workspace);

        // clear current neuron
        setCurrentNeuron(null);

        // notify listeners
        workspaceChangedSignal.emit(workspace);
        neuronChangedSignal.emit(null);

    }

}
