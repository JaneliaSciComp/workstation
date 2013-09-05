package org.janelia.it.FlyWorkstation.gui.slice_viewer.annotation;


// workstation imports


import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.skeleton.Anchor;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.skeleton.Skeleton;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;

import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.FlyWorkstation.signal.Slot1;
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

    public Slot1<Anchor> deleteLinkRequestedSlot = new Slot1<Anchor>() {
        @Override
        public void execute(Anchor anchor) {
            deleteLink(anchor.getGuid());
        }
    };

    public Slot1<Anchor> deleteSubtreeRequestedSlot = new Slot1<Anchor>() {
        @Override
        public void execute(Anchor anchor) {
            deleteSubTree(anchor.getGuid());
        }
    };

    public Slot1<Anchor> moveAnchorRequestedSlot = new Slot1<Anchor>() {
        @Override
        public void execute(Anchor anchor) {
            moveAnnotation(anchor.getGuid(), anchor.getLocation());
        }
    };

    public Slot1<Anchor> selectAnnotationSlot = new Slot1<Anchor>() {
        @Override
        public void execute(Anchor anchor) {
            if (anchor != null) {
                selectNeuronFromAnnotation(anchor.getGuid());
            }
        }
    };


    public AnnotationManager(AnnotationModel annotationModel) {
        this.annotationModel = annotationModel;

        modelMgr = ModelMgr.getModelMgr();

    }

    public Entity getInitialEntity() {
        return initialEntity;
    }

    public void setInitialEntity(final Entity initialEntity) {

        this.initialEntity = initialEntity;

        if (initialEntity.getEntityType().getName().equals(EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE)) {
            // currently nothing to do for this case
        }

        else if (initialEntity.getEntityType().getName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {

            SimpleWorker loader = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    // need to invalidate the cache and reload this entity; probably short-term, as
                    //  we should fix the underlying problem (saving data and not telling the cache!)
                    modelMgr.invalidateCache(initialEntity, true);
                    Entity entity = modelMgr.getEntityById(initialEntity.getId());

                    // now make sure the entity's children are fully loaded or
                    //  the workspace creation will fail
                    modelMgr.loadLazyEntity(entity, false);
                    TmWorkspace workspace = new TmWorkspace(entity);
                    annotationModel.loadWorkspace(workspace);
                }

                @Override
                protected  void hadSuccess() {
                    // no hadSuccess(); signals will be emitted in the loadWorkspace() call
                }

                @Override
                protected void hadError(Throwable error) {
                    SessionMgr.getSessionMgr().handleException(error);
                }
            };
            loader.execute();

        }


        // (eventually) update state to saved state (selection, visibility, etc)



    }
    

    // methods that are called by actions from the 2d view; should be not
    //  much more than what tool is active and where the click was;
    //  we are responsible for 

    public void addAnnotation(final Vec3 xyz, final Long parentID) {

        // get current workspace, etc.; if they don't exist, error
        if (annotationModel.getCurrentWorkspace() == null) {
            JOptionPane.showMessageDialog(null,
                "You must load a workspace before beginning annotation!",
                "No workspace!",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        final TmNeuron currentNeuron = annotationModel.getCurrentNeuron();
        if (currentNeuron == null) {
            JOptionPane.showMessageDialog(null,
                "You must select a neuron before beginning annotation!",
                "No neuron!",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // verify the parent annotation (if there is one) is in our neuron
        //  (probably temporary, it should auto-select or something, or perhaps
        //  not even care)
        if (parentID != null && !currentNeuron.getGeoAnnotationMap().containsKey(parentID)) {
            JOptionPane.showMessageDialog(null,
                    "Current neuron does not contain selected root annotation!",
                    "Wrong neuron!",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        SimpleWorker adder = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                if (parentID == null) {
                    // if parentID is null, it's a new root in current neuron

                    // this should probably not take the ws and neuron (assume current),
                    //  but it does for now
                    annotationModel.addRootAnnotation(annotationModel.getCurrentWorkspace(),
                        currentNeuron, xyz);

                } else {
                    // new node with existing parent

                    annotationModel.addChildAnnotation(currentNeuron,
                        currentNeuron.getGeoAnnotationMap().get(parentID), xyz);

                }
            }

            @Override
            protected void hadSuccess() {
                // nothing here now; signals will be emitted in annotationModel
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        adder.execute();

    }

    public void deleteLink(final Long annotationID) {
        if (annotationModel.getCurrentWorkspace() == null) {
            // dialog?

            return;
        } else {

            // verify it's a link and not a root or branch:
            TmGeoAnnotation annotation = annotationModel.getGeoAnnotationFromID(annotationID);
            if (annotation.getParent() == null || annotation.getChildren().size() > 1) {
                JOptionPane.showMessageDialog(null,
                        "This annotation is either a root (no parent) or branch (many children), not a link!",
                        "Not a link!",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }


            SimpleWorker deleter = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    annotationModel.deleteLink(annotationModel.getGeoAnnotationFromID(annotationID));
                }

                @Override
                protected void hadSuccess() {
                    // nothing here; annotationModel emits signals
                }

                @Override
                protected void hadError(Throwable error) {
                    SessionMgr.getSessionMgr().handleException(error);
                }
            };
            deleter.execute();
        }
    }

    public void deleteSubTree(final Long annotationID) {
        if (annotationModel.getCurrentWorkspace() == null) {
            // dialog?

            return;
        } else {
            SimpleWorker deleter = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    annotationModel.deleteSubTree(annotationModel.getGeoAnnotationFromID(annotationID));
                }

                @Override
                protected void hadSuccess() {
                    // nothing here; annotationModel emits signals
                }

                @Override
                protected void hadError(Throwable error) {
                    SessionMgr.getSessionMgr().handleException(error);
                }
            };
            deleter.execute();
        }
    }

    public void moveAnnotation(final Long annotationID, final Vec3 location) {
        if (annotationModel.getCurrentWorkspace() == null) {
            // dialog?

            return;
        } else {

            SimpleWorker mover = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    annotationModel.moveAnnotation(annotationID, location);
                }

                @Override
                protected void hadSuccess() {
                    // nothing here; annotationModel will emit signals
                }

                @Override
                protected void hadError(Throwable error) {
                    SessionMgr.getSessionMgr().handleException(error);
                }
            };
            mover.execute();

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
        final String neuronName = (String)JOptionPane.showInputDialog(
            null,
            "Neuron name:",
            "Create neuron",
            JOptionPane.PLAIN_MESSAGE,
            null,                           // icon
            null,                           // choice list; absent = freeform
            "new neuron");
        if (neuronName == null || neuronName.length() == 0) {
            return;
        }

        // validate neuron name?  are there any rules for entity names?


        // create it:
        SimpleWorker creator = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                annotationModel.createNeuron(neuronName);
            }

            @Override
            protected void hadSuccess() {
                // nothing here, annModel emits its own signals
            }

            @Override
            protected void hadError(Throwable error) {
                JOptionPane.showMessageDialog(null,
                        "Could not create neuron!",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        };
        creator.execute();

    }

    public void selectNeuronFromAnnotation(Long annotationID) {
        if (annotationID == null) {
            return;
        }

        TmNeuron neuron = annotationModel.getNeuronFromAnnotation(annotationID);
        annotationModel.setCurrentNeuron(neuron);
    }

    public void createWorkspace() {
        // if no sample loaded, error
        if (initialEntity == null) {
            JOptionPane.showMessageDialog(null,
                    "You must load a brain sample before creating a workspace!",
                    "No brain sample!",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // check that the entity *is* a workspace or brain sample, and get the sample ID:
        Long sampleID;
        if (initialEntity.getEntityType().getName().equals(EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE)) {
            sampleID = initialEntity.getId();
        } else if (initialEntity.getEntityType().getName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
            sampleID = annotationModel.getCurrentWorkspace().getSampleID();
        } else {
            JOptionPane.showMessageDialog(null,
                    "You must load a brain sample before creating a workspace!",
                    "No brain sample!",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // ask re: new workspace if one is active
        if (annotationModel.getCurrentWorkspace() != null) {
            int ans = JOptionPane.showConfirmDialog(null,
                    "You already have an active workspace!  Close and create another?",
                    "Workspace exists",
                    JOptionPane.YES_NO_OPTION);
            if (ans == JOptionPane.NO_OPTION) {
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

        // create it in another thread
        // there is no doublt a better way to get these parameters in:
        final String name = workspaceName;
        final Long ID = sampleID;
        SimpleWorker creator = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                // for now, we'll put the new workspace into a default, top-level folder
                //  named "Workspaces", which we will create if it does not exit; later,
                //  we'll create a dialog to let the user choose the location of the
                //  new workspace, and perhaps the brain sample, too
                Entity workspaceRootEntity = annotationModel.getOrCreateWorkspacesFolder();

                // now we can create the workspace (finally)
                // annotationModel.createWorkspace(workspaceRootEntity, sampleID, workspaceName);
                annotationModel.createWorkspace(workspaceRootEntity, ID, name);

            }

            @Override
            protected void hadSuccess() {
                // nothing here, signals emitted within annotationModel
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        creator.execute();

    }

}















