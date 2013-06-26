package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.annotation;



// workstation imports


import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;
import sun.awt.ModalityListener;

import javax.swing.*;


public class AnnotationManager
{

    // annotation model object
    private AnnotationModel annotationModel;
    
    private Entity initialEntity;

    // current workspace, brain sample
    // current neuron

    // current neurite (?)

    // current annotation




    public AnnotationManager(AnnotationModel annotationModel) {


        this.annotationModel = annotationModel;


    }


    public Entity getInitialEntity() {
        return initialEntity;
    }

    public void setInitialEntity(Entity initialEntity) {
        TmWorkspace workspace;

        if (initialEntity.getEntityType().getName().equals(EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE)) {
            // pass
        }

        else if (initialEntity.getEntityType().getName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
            // get associated brain sample and load it
            // not done yet

            // load the workspace itself
            try {
                workspace = new TmWorkspace(initialEntity);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            annotationModel.setCurrentWorkspace(workspace);
        }

        this.initialEntity = initialEntity;





        // (eventually) update state to saved state (selection, visibility, etc)



        // (now) update (populate) neuron list; or: model does this, notifies?




    }
    

    // methods that are called by actions from the 2d view; should be not
    //  much more than what tool is active and where the click was;
    //  we are responsible for 

    public void addAnnotation(Vec3 xyz) {

        // get current workspace, brain, neurite, etc.
        // if they don't exist, error

        // if an annotation is selected, it's the parent
        // if not, check for existing root (currently can't handle > 1);
        //  if no existing root, new root annotation; else, fail

        // annModel.addRootAnnotation(currentWorkspace, currentNeuron, xyz);

        // update stuff?  or does this happen automatically?
        //  - 2d view
        //  - neurite tree 

        // select new annotation
        // update selection listeners

    }

    public void createNeuron() {
        // is there a workspace?  if not, fail; actually, doesn't the model know?
        //  yes; who should test?  who should pop up UI feedback to user?
        //  model shouldn't, but should annMgr? 

        if (annotationModel.getCurrentWorkspace() == null) {
            // dialog?

            return;
        }

        // ask user for name; you *can* rename on the sidebar, but that will 
        //  trigger a need to reload the slice viewer, so don't make the user go 
        //  through that
        String neuronName = (String)JOptionPane.showInputDialog(
            null,
            "Neuron name:",
            "Create neuron",
            JOptionPane.PLAIN_MESSAGE,
            null,                           // icon
            null,                           // choice list; absent = freeform
            "new neuron");

        // validate neuron name;  are there any rules for entity names?
        if ((neuronName == null) || (neuronName.length() == 0)) {
            neuronName = "new neuron";
        }



        // create it:
        TmNeuron neuron = annotationModel.createNeuron(neuronName);
        if (neuron == null) {
            // dialog
            // failure dialog
            JOptionPane.showMessageDialog(null, 
                "Could not create neuron!",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }


    }

    public void createWorkspace() {

        // are we in a workspace now?  if so, is it saved before 
        //  we create a new one?
        if (annotationModel.getCurrentWorkspace() != null) {
            // dialog
        }


        // get current entity; make sure it's a sample, or if it's a workspace,
        //  get its sample




        // dialog: where to put workspace?  (must be a folder?)
        // EntityConstants.TYPE_FOLDER




        // create it; you can rename entities in the sidebar, so don't bother prompting
        //  for a name when you created it

        // for testing, hardcode both the parent and sample entities, until I can have
        //  the user provide them;

        ModelMgr modelMgr = ModelMgr.getModelMgr();
        Entity parentEntity;
        Entity sampleEntity;
        try {
            // olbris tests folder:
            parentEntity = modelMgr.getEntityById(1884682659553083568L);
            // 2013-04-01 data:
            sampleEntity = modelMgr.getEntityById(1872744417371095216L);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        TmWorkspace workspace = annotationModel.createWorkspace(parentEntity, sampleEntity, "untitled");
        if (workspace == null) {
            // failure dialog
            JOptionPane.showMessageDialog(null, 
                "Could not create workspace!",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }

        // System.out.println("workspace created with ID " + workspace.getId());


    }

}















