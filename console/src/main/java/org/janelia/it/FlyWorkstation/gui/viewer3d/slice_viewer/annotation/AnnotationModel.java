package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.annotation;


// workstation imports

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;

import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.Signal1;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;

import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;


// Java imports
import java.util.ArrayList;
import java.util.List;



public class AnnotationModel 
{
    private ModelMgr modelMgr;
    private SessionMgr sessionMgr;

    private TmWorkspace currentWorkspace;
    private TmNeuron currentNeuron;

    // signals
    public Signal1<TmWorkspace> workspaceChangedSignal = new Signal1<TmWorkspace>();


    public TmWorkspace getCurrentWorkspace() {
        return currentWorkspace;
    }

    public void setCurrentWorkspace(TmWorkspace currentWorkspace) {
        this.currentWorkspace = currentWorkspace;
        loadWorkspace(currentWorkspace);
    }

    public AnnotationModel() {


        /* we've got to do two things here; first, create empty stuff and 
            hook up as needed; second, we need to retrieve what just happened
            to start us up (what the user clicked on), and load the data
            corresponding to the context we're in

           for testing, we'll hard-code some stuff
        */


        // set up
        // presumably this is where we'll grab the Model Manager?
        modelMgr = ModelMgr.getModelMgr();
        sessionMgr = SessionMgr.getSessionMgr();

         // presumably this is where we'll grab the Model Manager?


        // this might be a valid sample ID: 1872744417572421808
        // public List<TmWorkspaceDescriptor> getWorkspacesForBrainSample(Long brainSampleId, String ownerKey)
        /* 
        // sample code:
        try {
            List<TmWorkspaceDescriptor> temp = modelMgr.getWorkspacesForBrainSample(1872744417572421808L, "clackn");
            if (temp == null) {
                System.out.println("getWorkspacesForBrainSample() returned null");
            } else {
                System.out.println("got " + temp.size() + " workspaces");
            }

        }
        catch (Exception e) {
            // pass, testing
            System.err.println("ModelMgr Exception: " + e.getMessage());
        }
        */


        // get current data

    }

    public TmNeuron createNeuron(String name) {

        try {
            currentNeuron = modelMgr.createTiledMicroscopeNeuron(currentWorkspace.getId(), name);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        // notify listeners


        return currentNeuron;
        
    }

    public TmWorkspace createWorkspace(Entity parentEntity, Entity brainSample, String name) {
        TmWorkspace workspace;

        // do stuff
        Subject subject = sessionMgr.getSubject();

        try {
            workspace = modelMgr.createTiledMicroscopeWorkspace(parentEntity.getId(),
                brainSample.getId(), name, subject.getKey());
        }  catch (Exception e) {
            e.printStackTrace();
            return null;
        }



        // trigger workspace load
        loadWorkspace(workspace);


        return workspace;

    }

    public void addRootAnnotation(TmWorkspace workspace, TmNeuron neuron, Vec3 xyz) {

        // talk to the facade layer to do the add

        // notify interested parties
        // what's the pattern going to be?  pass along the changed neuron?  or
        //  somehow indicate the new annotation?

           

    }

    public boolean exportSWC(String filename) {

        // probably will actually get more info passed in, eg, which neurite to export

        return false;

    }

    private void loadWorkspace(TmWorkspace workspace) {
        currentWorkspace = workspace;

        // notify listeners
        workspaceChangedSignal.emit(currentWorkspace);

    }

}
