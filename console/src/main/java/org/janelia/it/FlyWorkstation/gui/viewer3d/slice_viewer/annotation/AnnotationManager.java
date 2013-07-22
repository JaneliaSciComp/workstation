package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.annotation;



// workstation imports


import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;

import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.Slot1;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.skeleton.Anchor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.skeleton.Skeleton;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;


import javax.swing.*;


public class AnnotationManager
{

    ModelMgr modelMgr;

    // annotation model object
    private AnnotationModel annotationModel;
    
    private Entity initialEntity;

    // signals & slots
    public Slot1<Skeleton.AnchorSeed> addAnchorRequestedSlot = new Slot1<Skeleton.AnchorSeed>() {
        @Override
        public void execute(Skeleton.AnchorSeed seed) {
            addAnnotation(seed.getLocation(), seed.getParentGuid());
        }
    };

    public Slot1<Anchor> deleteAnchorRequestedSlot = new Slot1<Anchor>() {
        @Override
        public void execute(Anchor anchor) {
            // currently delete subtree, probably change later
            deleteSubTree(anchor.getGuid());
        }
    };

    // constants
    public static final String WORKSPACES_FOLDER_NAME = "Workspaces";



    public AnnotationManager(AnnotationModel annotationModel) {


        this.annotationModel = annotationModel;

        modelMgr = ModelMgr.getModelMgr();



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
            annotationModel.loadWorkspace(workspace);
        }

        this.initialEntity = initialEntity;





        // (eventually) update state to saved state (selection, visibility, etc)



        // (now) update (populate) neuron list; or: model does this, notifies?




    }
    

    // methods that are called by actions from the 2d view; should be not
    //  much more than what tool is active and where the click was;
    //  we are responsible for 

    public void addAnnotation(Vec3 xyz, Long parentID) {

        // get current workspace, etc.; if they don't exist, error
        if (annotationModel.getCurrentWorkspace() == null) {
            JOptionPane.showMessageDialog(null,
                "You must load a workspace before beginning annotation!",
                "No workspace!",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        TmNeuron currentNeuron = annotationModel.getCurrentNeuron();
        if (currentNeuron == null) {
            JOptionPane.showMessageDialog(null,
                "You must select a neuron before beginning annotation!",
                "No neuron!",
                JOptionPane.ERROR_MESSAGE);
            return;
        }


        // if parentID is null, it's a new root in current neuron
        if (parentID == null) {
            // new root in current neuron:

            // currently can't handle > 1 root per neuron
            if (currentNeuron.getRootAnnotation() != null) {
                JOptionPane.showMessageDialog(null,
                        "This neuron already has a root annotation!  (This restriction will be remored in the future.)",
                        "Already has a root!",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // this should probably not take the ws and neuron (assume current), but
            //  we're testing:
            annotationModel.addRootAnnotation(annotationModel.getCurrentWorkspace(),
                currentNeuron, xyz);


        } else {
            // new node with existing parent

            // verify the supposed parent annotation is in our neuron
            //  (probably temporary; at some point, we'll have to handle display of,
            //  and smooth switching of operations between, different neurons, triggered
            //  from the 2D view)
            if (!currentNeuron.getGeoAnnotationMap().containsKey(parentID)) {
                JOptionPane.showMessageDialog(null,
                        "Current neuron does not contain selected root annotation!",
                        "Wrong neuron!",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            annotationModel.addChildAnnotation(currentNeuron,
                currentNeuron.getGeoAnnotationMap().get(parentID), xyz);

        }

        // select new annotation (?) (currently marked graphically?)



    }

    public void deleteSubTree(Long annotationID) {
        TmWorkspace workspace = annotationModel.getCurrentWorkspace();
        if (workspace == null) {
            // dialog?

            return;
        } else {
            annotationModel.deleteSubTree(annotationModel.getGeoAnnotationFromID(annotationID));
        }
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
        if (neuronName == null) {
            return;
        }

        // validate neuron name;  are there any rules for entity names?
        if (neuronName.length() == 0) {
            neuronName = "new neuron";
        }


        // create it:
        if (!annotationModel.createNeuron(neuronName)) {
            JOptionPane.showMessageDialog(null, 
                "Could not create neuron!",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }

    }

    public void createWorkspace() {

        // first we need to figure out the brain sample; the user may
        //  open the slice viewer from either a brain sample or a workspace; if
        //  it's the latter, grab its brain sample
        // (currently can't open Slice Viewer without an initial entity)

        // NOTE: ask the user if you're creating a new workspace when one is
        //  already active
        Entity sampleEntity;
        if (annotationModel.getCurrentWorkspace() != null) {
            // dialog
            int ans = JOptionPane.showConfirmDialog(null,
                "You already have an active workspace!  Close and create another?",
                "Workspace exists",
                JOptionPane.YES_NO_OPTION);
            if (ans == JOptionPane.YES_OPTION) {
                Long sampleID = annotationModel.getCurrentWorkspace().getSampleID();
                try {
                    sampleEntity = modelMgr.getEntityById(sampleID);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            } else {
                // users says no
                return;
            }
        } else {
            // no workspace, look at initial entity; it must be a brain sample!
            if (!initialEntity.getEntityType().getName().equals(EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE)) {
                JOptionPane.showMessageDialog(null,
                        "You must load a brain sample before creating a workspace!",
                        "No brain sample!",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            sampleEntity= initialEntity;
        }

        // for now, we'll put the new workspace into a default, top-level folder
        //  named "Workspaces", which we will create if it does not exit; later,
        //  we'll create a dialog to let the user choose the location of the
        //  new workspace, and perhaps the brain sample, too
        Entity workspaceRootEntity;
        try {
            workspaceRootEntity = modelMgr.getCommonRootEntityByName(WORKSPACES_FOLDER_NAME);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        if (workspaceRootEntity == null) {
            try {
                workspaceRootEntity = modelMgr.createCommonRoot(WORKSPACES_FOLDER_NAME);
            } catch (Exception e) {
                e.printStackTrace();
                // fail: dialog
                JOptionPane.showMessageDialog(null,
                    "Could not create Workspaces top-level folder!",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // get a name for the new workspace and validate (are there any rules for entity names?)
        String workspaceName = (String)JOptionPane.showInputDialog(
            null,
            "Workspace name:",
            "Create workspace",
            JOptionPane.PLAIN_MESSAGE,
            null,                           // icon
            null,                           // choice list; absent = freeform
            "new workspace");
        if ((workspaceName == null) || (workspaceName.length() == 0)) {
            workspaceName = "new workspace";
        }

        // create it
        if (!annotationModel.createWorkspace(workspaceRootEntity, sampleEntity, workspaceName)) {
            JOptionPane.showMessageDialog(null, 
                "Could not create workspace!",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }

    }

}















