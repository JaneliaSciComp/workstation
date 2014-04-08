package org.janelia.it.FlyWorkstation.gui.slice_viewer.annotation;

import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.QuadViewUi;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.skeleton.Anchor;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.skeleton.Skeleton;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;

import org.janelia.it.FlyWorkstation.octree.ZoomedVoxelIndex;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.FlyWorkstation.signal.Slot;
import org.janelia.it.FlyWorkstation.signal.Slot1;
import org.janelia.it.FlyWorkstation.tracing.AnchoredVoxelPath;
import org.janelia.it.FlyWorkstation.tracing.PathTraceToParentRequest;
import org.janelia.it.FlyWorkstation.tracing.PathTraceToParentWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class AnnotationManager
/*
this class is the middleman between the UI and the model.  first, the UI makes naive requests 
(eg, add annotation).  then this class determines if the request is valid (eg, can't add
if no neuron), popping dialogs if needed.  lastly, this class gathers and/or reformats info as
needed to actually make the call to the back end, usually spinning off a worker thread to
do so.  

this class's slots are usually connected to various UI signals, and its signals typically 
hook up to AnnotationModel slots.  this class has no responsibilities in notifying UI 
elements of what's been done; that's handled by signals emitted from AnnotationModel.
*/
{

    ModelMgr modelMgr;

    // annotation model object
    private AnnotationModel annotationModel;

    // quad view ui object
    private QuadViewUi quadViewUi;

    private Entity initialEntity;

    // ----- constants
    // AUTOMATIC_TRACING_TIMEOUT for automatic tracing in seconds
    private static final double AUTOMATIC_TRACING_TIMEOUT = 10.0;


    // ----- slots

    public Slot1<URL> onVolumeLoadedSlot = new Slot1<URL>() {
        @Override
        public void execute(URL url) {
            onVolumeLoaded();
        }
    };

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

    public Slot1<Anchor> splitAnchorRequestedSlot = new Slot1<Anchor>() {
        @Override
        public void execute(Anchor anchor) {
            splitAnchor(anchor.getGuid());
        }
    };

    public Slot1<Anchor> rerootNeuriteRequestedSlot = new Slot1<Anchor>() {
        @Override
        public void execute(Anchor anchor) {
            rerootNeurite(anchor.getGuid());
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

    public Slot1<PathTraceToParentRequest> tracePathRequestedSlot = new Slot1<PathTraceToParentRequest>() {
        @Override
        public void execute(PathTraceToParentRequest request) {
            tracePathToParent(request);
        }
    };

    public Slot1<AnchoredVoxelPath> addPathRequestedSlot = new Slot1<AnchoredVoxelPath>() {
        @Override
        public void execute(AnchoredVoxelPath voxelPath) {
            if (voxelPath != null) {
                TmAnchoredPathEndpoints endpoints = new TmAnchoredPathEndpoints(
                        voxelPath.getSegmentIndex().getAnchor1Guid(),
                        voxelPath.getSegmentIndex().getAnchor2Guid());
                List<List<Integer>> pointList = new ArrayList<List<Integer>>();
                for (ZoomedVoxelIndex zvi: voxelPath.getPath()) {
                    if (zvi.getZoomLevel().getZoomOutFactor() != 1) {
                        // compromise between me and CB: I don't want zoom levels in db, so 
                        //  if I get one that's not unzoomed, I don't have to handle it
                        JOptionPane.showMessageDialog(null,
                            "Unexpected zoom level found; path not displayed.",
                            "Unexpected zoom!",
                            JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    List<Integer> tempList = new ArrayList<Integer>();
                    tempList.add(zvi.getX());
                    tempList.add(zvi.getY());
                    tempList.add(zvi.getZ());
                    pointList.add(tempList);
                }
                addAnchoredPath(endpoints, pointList);
            }
        }
    };

    public Slot closeWorkspaceRequestedSlot = new Slot() {
        @Override
        public void execute() {
            setInitialEntity(null);
        }
    };

    public AnnotationManager(AnnotationModel annotationModel, QuadViewUi quadViewUi) {
        this.annotationModel = annotationModel;
        this.quadViewUi = quadViewUi;
        modelMgr = ModelMgr.getModelMgr();
    }

    public Entity getInitialEntity() {
        return initialEntity;
    }

    /**
     * @param initialEntity = entity the user right-clicked on to start the slice viewer
     */
    public void setInitialEntity(final Entity initialEntity) {
        this.initialEntity = initialEntity;
    }

    /**
     * called when volume is *finished* loading (so as to avoid race conditions);
     * relies on initial entity being properly set already
     */
    public void onVolumeLoaded() {

        if (initialEntity == null) {
            // this is a request to clear the workspace
            SimpleWorker closer = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    annotationModel.loadWorkspace(null);
                }

                @Override
                protected void hadSuccess() {
                    // sends its own signals
                }

                @Override
                protected void hadError(Throwable error) {
                    SessionMgr.getSessionMgr().handleException(error);
                }
            };
            closer.execute();

        } else if (initialEntity.getEntityTypeName().equals(EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE)) {
            // if it's a bare sample, we don't have anything to do

        } else if (initialEntity.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
            SimpleWorker loader = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    // at this point, we know the entity is a workspace, so:
                    TmWorkspace workspace = modelMgr.loadWorkspace(initialEntity.getId());
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

        // (eventually) update state to saved state (selection, visibility, etc);
        //  actually, although it should happen at about the time this method is called,
        //  it may be better to make it a different method

    }

    // ----- methods called from UI
    // these methods are called by actions from the 2d view; should be not
    //  much more than what tool is active and where the click was;
    //  we are responsible for everything else

    /**
     * add an annotation at the given location with the given parent ID;
     * if no parent, pass in null
     */
    public void addAnnotation(final Vec3 xyz, final Long parentID) {
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

        // verify that the parent annotation (if there is one) is in our neuron;
        //  this is probably no longer needed, as the neuron ought to be selected
        //  when the parent annotation is selected
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
                    annotationModel.addRootAnnotation(currentNeuron, xyz);
                } else {
                    annotationModel.addChildAnnotation(
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

    /**
     * delete the annotation with the input ID; the annotation must be a "link", which
     * is an annotation that is not a root (no parent) or branch point (many children);
     * in other words, it's an end point, or an annotation with a parent and single child
     * that can be connected up unambiguously
     */
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

    /**
     * delete the annotation with the input ID, and delete all of its descendants
     */
    public void deleteSubTree(final Long annotationID) {
        if (annotationModel.getCurrentWorkspace() == null) {
            // dialog?
            return;
        } else {

            // if more than a handful of nodes, ask the user if they are sure (we have
            //  no undo right now!)
            int nAnnotations = annotationModel.getGeoAnnotationFromID(annotationID).getSubTreeList().size();
            if (nAnnotations >= 5) {
                int ans =  JOptionPane.showConfirmDialog(null,
                        String.format("Selected subtree has %d children; delete?", nAnnotations),
                        "Delete subtree?",
                        JOptionPane.OK_CANCEL_OPTION);
                if (ans != JOptionPane.OK_OPTION) {
                    return;
                }
            }

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

    /**
     * move the annotation with the input ID to the input location
     */
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

    /**
     * place a new annotation near the annotation with the input ID; place it
     * "nearby" in the direction of its parent if it has one; if it's a root
     * annotation with one child, place it in the direction of the child instead;
     * if it's a root with many children, it's an error, since there is no
     * unambiguous location to place the new anchor
     */
    public void splitAnchor(Long annotationID) {
        if (annotationModel.getCurrentWorkspace() == null) {
            // dialog?
            return;
        }

        // can't split a root if it has multiple children (ambiguous):
        final TmGeoAnnotation annotation = annotationModel.getGeoAnnotationFromID(annotationID);
        if (annotation.getParent() == null && annotation.getChildren().size() != 1) {
            JOptionPane.showMessageDialog(null,
                    "Cannot split root annotation with multiple children (ambiguous)!",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        SimpleWorker splitter = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                annotationModel.splitAnnotation(annotation);
            }

            @Override
            protected void hadSuccess() {
                // nothing here, model emits signals
            }

            @Override
            protected void hadError(Throwable error) {
                JOptionPane.showMessageDialog(null,
                        "Could not split anchor!",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        };
        splitter.execute();
    }

    public void rerootNeurite(final Long newRootAnnotationID) {
        if (annotationModel.getCurrentWorkspace() == null) {
            return;
        }

        SimpleWorker rerooter = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                annotationModel.rerootNeurite(newRootAnnotationID);
            }

            @Override
            protected void hadSuccess() {
                // nothing here, model emits signals
            }

            @Override
            protected void hadError(Throwable error) {
                JOptionPane.showMessageDialog(null,
                        "Could not reroot neurite!",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        };
        rerooter.execute();
    }

    /**
     * add an anchored path; not much to check, as the UI needs to check it even before
     * the request gets here
     */
    public void addAnchoredPath(final TmAnchoredPathEndpoints endpoints, final List<List<Integer>> points) {
        if (annotationModel.getCurrentWorkspace() == null) {
            // dialog?
            return;
        }

        SimpleWorker adder = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                annotationModel.addAnchoredPath(endpoints, points);
            }

            @Override
            protected void hadSuccess() {
                // nothing here; model sends its own signals
            }

            @Override
            protected void hadError(Throwable error) {
                JOptionPane.showMessageDialog(null,
                        "Could not add anchored path!",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        };
        adder.execute();
    }

    /**
     * create a new neuron in the current workspace, prompting for name
     */
    public void createNeuron() {
        if (annotationModel.getCurrentWorkspace() == null) {
            // dialog?
            return;
        }

        // use a standard neuron name; the user can rename later
        final String neuronName = getNeuronName(annotationModel.getCurrentWorkspace());

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

    public void deleteCurrentNeuron() {
        TmNeuron neuron = annotationModel.getCurrentNeuron();
        if (neuron == null) {
            return;
        }

        int nAnnotations = neuron.getGeoAnnotationMap().size();
        int ans =  JOptionPane.showConfirmDialog(null,
                String.format("%s has %d nodes; delete?", neuron.getName(), nAnnotations),
                "Delete neuron?",
                JOptionPane.OK_CANCEL_OPTION);
        if (ans == JOptionPane.OK_OPTION) {
            SimpleWorker deleter = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    annotationModel.deleteCurrentNeuron();
                }

                @Override
                protected void hadSuccess() {
                    // nothing here; model sends its own signals
                }

                @Override
                protected void hadError(Throwable error) {
                    JOptionPane.showMessageDialog(null,
                            "Could not delete current neuron!",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            };
            deleter.execute();
        }

    }

    /**
     * rename the currently selected neuron
     */
    public void renameNeuron() {
        final TmNeuron neuron = annotationModel.getCurrentNeuron();
        if (neuron == null) {
            JOptionPane.showMessageDialog(null,
                    "No selected neuron!",
                    "No neuron!",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        final String neuronName = (String)JOptionPane.showInputDialog(
                null,
                "Neuron name:",
                "Rename neuron",
                JOptionPane.PLAIN_MESSAGE,
                null,                           // icon
                null,                           // choice list; absent = freeform
                neuron.getName());
        if (neuronName == null || neuronName.length() == 0) {
            return;
        }

        // validate neuron name?  are there any rules for entity names?

        SimpleWorker renamer = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                annotationModel.renameCurrentNeuron(neuronName);
            }

            @Override
            protected void hadSuccess() {
                // nothing here, annModel emits its own signals
            }

            @Override
            protected void hadError(Throwable error) {
                JOptionPane.showMessageDialog(null,
                        "Could not rename neuron!",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        };
        renamer.execute();

    }

    /**
     * given a workspace, return a new generic neuron name (probably something
     * like "New neuron 12", where the integer is based on whatever similarly
     * named neurons exist already)
     */
    private String getNeuronName(TmWorkspace workspace) {
        // go through existing neuron names; try to parse against
        //  standard template; create list of integers found
        ArrayList<Long> intList = new ArrayList<Long>();
        Pattern pattern = Pattern.compile("New neuron ([0-9]+)");
        for (TmNeuron neuron: workspace.getNeuronList()) {
            Matcher matcher = pattern.matcher(neuron.getName());
            if (matcher.matches()) {
                intList.add(Long.parseLong(matcher.group(1)));
            }
        }

        // construct new name from standard template; use largest integer
        //  found + 1; starting with max = 0 has the effect of always starting
        //  at at least 1, if anyone has named their neurons with negative numbers
        Long maximum = 0L;
        if (intList.size() > 0) {
            for (Long l: intList) {
                if (l > maximum) {
                    maximum = l;
                }
            }
        }
        return String.format("New neuron %d", maximum + 1);
    }


    /**
     * given an annotation ID, select (make current) the neuron it belongs to
     */
    public void selectNeuronFromAnnotation(Long annotationID) {
        if (annotationID == null) {
            return;
        }

        TmNeuron neuron = annotationModel.getNeuronFromAnnotation(annotationID);
        annotationModel.setCurrentNeuron(neuron);
    }

    /**
     * create a new workspace with the currently loaded sample
     */
    public void createWorkspace() {
        // if no sample loaded, error
        if (initialEntity == null) {
            JOptionPane.showMessageDialog(null,
                    "You must load a brain sample entity before creating a workspace!",
                    "No brain sample!",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // check that the entity *is* a workspace or brain sample, and get the sample ID:
        Long sampleID;
        if (initialEntity.getEntityTypeName().equals(EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE)) {
            sampleID = initialEntity.getId();
        } else if (initialEntity.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
            sampleID = annotationModel.getCurrentWorkspace().getSampleID();
        } else {
            JOptionPane.showMessageDialog(null,
                    "You must load a brain sample before creating a workspace!",
                    "No brain sample!",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // ask the user if they really want a new workspace if one is active
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
        // this is all the validation we have right now...
        if ((workspaceName == null) || (workspaceName.length() == 0)) {
            workspaceName = "new workspace";
        }

        // create it in another thread
        // there is no doubt a better way to get these parameters in:
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

    public void setGlobalAnnotationColor(final Color color) {

        SimpleWorker setter = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                annotationModel.setGlobalAnnotationColor(color);
            }

            @Override
            protected void hadSuccess() {
                // nothing; signals will be sent
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        setter.execute();
    }

    public void saveColorModel() {
        SimpleWorker saver = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                annotationModel.setPreference(AnnotationsConstants.PREF_COLOR_MODEL, quadViewUi.imageColorModelAsString());
            }

            @Override
            protected void hadSuccess() {
                // nothing here
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        saver.execute();

    }

    public void setAutomaticTracing(final boolean state) {
        SimpleWorker saver = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                annotationModel.setPreference(AnnotationsConstants.PREF_AUTOMATIC_TRACING,
                        String.valueOf(state));
            }

            @Override
            protected void hadSuccess() {
                // nothing here
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        saver.execute();

    }

    private void tracePathToParent(PathTraceToParentRequest request) {
        TmGeoAnnotation annotation = annotationModel.getGeoAnnotationFromID(request.getAnchorGuid1());
        if (annotation.getParent() == null)  {
            // no parent, no tracing
            return;
        }

        TmGeoAnnotation parent = annotation.getParent();
        request.setAnchorGuid2(parent.getId());
        request.setXyz1(new Vec3(annotation.getX(), annotation.getY(), annotation.getZ()));
        request.setXyz2(new Vec3(parent.getX(), parent.getY(), parent.getZ()));

        // tracing:
        PathTraceToParentWorker worker = new PathTraceToParentWorker(request, AUTOMATIC_TRACING_TIMEOUT);
        worker.pathTracedSignal.connect(addPathRequestedSlot);
        worker.execute();

        // we'd really prefer to see this worker's status in the Progress Monitor, but as of
        //  Jan. 2014, that monitor's window repositions itself and comes to front on every
        //  new task, so it's far too intrusive to be used for our purpose; see FW-2191
        // worker.executeWithEvents();

    }
}

