package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import com.fasterxml.jackson.databind.JsonNode;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.large_volume_viewer.QuadViewUi;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Anchor;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Skeleton;

import org.janelia.it.workstation.shared.workers.BackgroundWorker;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.workstation.signal.Slot;
import org.janelia.it.workstation.signal.Slot1;
import org.janelia.it.workstation.tracing.AnchoredVoxelPath;
import org.janelia.it.workstation.tracing.PathTraceToParentRequest;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;
import org.janelia.it.workstation.tracing.PathTraceToParentWorker;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.janelia.it.workstation.gui.large_volume_viewer.ComponentUtil;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.AnchorListener;
import org.janelia.it.workstation.gui.large_volume_viewer.TileFormat;
import org.janelia.it.workstation.gui.large_volume_viewer.TileServer;
import org.janelia.it.workstation.gui.large_volume_viewer.UpdateAnchorListener;
import org.janelia.it.workstation.tracing.VoxelPosition;

public class AnnotationManager implements AnchorListener, UpdateAnchorListener /**
 * this class is the middleman between the UI and the model. first, the UI makes
 * naive requests (eg, add annotation). then this class determines if the
 * request is valid (eg, can't add if no neuron), popping dialogs if needed.
 * lastly, this class gathers and/or reformats info as needed to actually make
 * the call to the back end, usually spinning off a worker thread to do so.
 *
 * this class's slots are usually connected to various UI signals, and its
 * signals typically hook up to AnnotationModel slots. this class has no
 * responsibilities in notifying UI elements of what's been done; that's handled
 * by signals emitted from AnnotationModel.
 */
{

    ModelMgr modelMgr;

    // annotation model object
    private AnnotationModel annotationModel;

    // quad view ui object
    private QuadViewUi quadViewUi;

    private Entity initialEntity;

    private TileServer tileServer;

    // ----- constants
    // AUTOMATIC_TRACING_TIMEOUT for automatic tracing in seconds
    private static final double AUTOMATIC_TRACING_TIMEOUT = 10.0;

    // when dragging to merge, how close in pixels (squared) to trigger
    //  a merge instead of a move
    // this distance chosen by trial and error; I annotated a neuron
    //  at what seemed like a reasonable zoom level, and I experimented
    //  until the distance threshold seemed right
    private static final double DRAG_MERGE_THRESHOLD_SQUARED = 250.0;

    //-----------------------------------IMPLEMENT AnchorListener
    @Override
    public void deleteSubtreeRequested(Anchor anchor) {
        if (anchor != null) {
            deleteSubTree(anchor.getGuid());
        } else {
            int x = 0;
        }
    }

    @Override
    public void splitAnchorRequested(Anchor anchor) {
        splitAnchor(anchor.getGuid());
    }

    @Override
    public void rerootNeuriteRequested(Anchor anchor) {
        rerootNeurite(anchor.getGuid());
    }

    @Override
    public void splitNeuriteRequested(Anchor anchor) {
        splitNeurite(anchor.getGuid());
    }

    @Override
    public void deleteLinkRequested(Anchor anchor) {
        deleteLink(anchor.getGuid());
    }

    @Override
    public void addEditNoteRequested(Anchor anchor) {
        addEditNote(anchor.getGuid());
    }

    //-----------------------------IMPLEMENT UpdateAnchorListener
    @Override
    public void update(Anchor anchor) {
        if (anchor != null) {
            selectNeuronFromAnnotation(anchor.getGuid());
        }
    }

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

    public Slot1<Anchor> moveAnchorRequestedSlot = new Slot1<Anchor>() {
        @Override
        public void execute(Anchor anchor) {

            // find closest to new anchor location that isn't the annotation already
            //  associated with anchor
            TileFormat tf = getTileFormat();
            Vec3 anchorVoxLoc = tf.voxelVec3ForMicronVec3(anchor.getLocation());
            TmGeoAnnotation closest = annotationModel.getClosestAnnotation(anchorVoxLoc,
                    annotationModel.getGeoAnnotationFromID(anchor.getGuid()));

            // check distance and other restrictions
            if (closest != null && canMergeNeurite(anchor, closest)) {
                // System.out.println("merging " + anchor.getGuid() + " to " + anchor.getLocation());
                mergeNeurite(anchor.getGuid(), closest.getId());
            } else {
                // System.out.println("moving " + anchor.getGuid() + " to " + anchor.getLocation());
                moveAnnotation(anchor.getGuid(), anchor.getLocation());
            }
        }
    };

//    public Slot1<Anchor> selectAnnotationSlot = new Slot1<Anchor>() {
//        @Override
//        public void execute(Anchor anchor) {
//            if (anchor != null) {
//                selectNeuronFromAnnotation(anchor.getGuid());
//            }
//        }
//    };

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
                List<List<Integer>> pointList = new ArrayList<>();
                for (VoxelPosition vp : voxelPath.getPath()) {
                    List<Integer> tempList = new ArrayList<>();
                    tempList.add(vp.getX());
                    tempList.add(vp.getY());
                    tempList.add(vp.getZ());
                    pointList.add(tempList);
                }
                addAnchoredPath(endpoints, pointList);
            }
        }
    };

    public Slot1<Anchor> addEditNoteRequestedSlot = new Slot1<Anchor>() {
        @Override
        public void execute(Anchor anchor) {
            addEditNote(anchor.getGuid());
        }
    };

    public Slot1<TmGeoAnnotation> editNoteRequestedSlot = new Slot1<TmGeoAnnotation>() {
        @Override
        public void execute(TmGeoAnnotation ann) {
            addEditNote(ann.getId());
        }
    };

    public Slot closeWorkspaceRequestedSlot = new Slot() {
        @Override
        public void execute() {
            setInitialEntity(null);
        }
    };

    public AnnotationManager(AnnotationModel annotationModel, QuadViewUi quadViewUi, TileServer tileServer) {
        this.annotationModel = annotationModel;
        this.quadViewUi = quadViewUi;
        this.tileServer = tileServer;
        modelMgr = ModelMgr.getModelMgr();
    }

    public TileFormat getTileFormat() {
        return tileServer.getLoadAdapter().getTileFormat();
    }

    public Entity getInitialEntity() {
        return initialEntity;
    }

    /**
     * @param initialEntity = entity the user right-clicked on to start the
     * large volume viewer
     */
    public void setInitialEntity(final Entity initialEntity) {
        this.initialEntity = initialEntity;
    }

    /**
     * called when volume is *finished* loading (so as to avoid race
     * conditions); relies on initial entity being properly set already
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
                protected void hadSuccess() {
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
     * add an annotation at the given location with the given parent ID; if no
     * parent, pass in null
     */
    public void addAnnotation(final Vec3 xyz, final Long parentID) {
        if (annotationModel.getCurrentWorkspace() == null) {
            presentError(
                    "You must load a workspace before beginning annotation!",
                    "No workspace!");
            return;
        }

        final TmNeuron currentNeuron = annotationModel.getCurrentNeuron();
        if (currentNeuron == null) {
            presentError(
                    "You must select a neuron before beginning annotation!",
                    "No neuron!");
            return;
        }

        // verify that the parent annotation (if there is one) is in our neuron;
        //  this is probably no longer needed, as the neuron ought to be selected
        //  when the parent annotation is selected
        if (parentID != null && !currentNeuron.getGeoAnnotationMap().containsKey(parentID)) {
            presentError(
                    "Current neuron does not contain selected root annotation!",
                    "Wrong neuron!");
            return;
        }

        SimpleWorker adder = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                Vec3 finalLocation;
                if (annotationModel.automatedRefinementEnabled()) {
                    // Stopwatch stopwatch = new Stopwatch();
                    // stopwatch.start();
                    PointRefiner refiner = new PointRefiner(quadViewUi.getSubvolumeProvider());
                    finalLocation = refiner.refine(xyz);
                    // stopwatch.stop();
                    // System.out.println("refined annotation; elapsed time = " + stopwatch.toString());

                    // System.out.println("add annotation: input point " + xyz);
                    // System.out.println("add annotation: refined point " + finalLocation);
                } else {
                    finalLocation = xyz;
                }

                // Stopwatch stopwatch = new Stopwatch();
                // stopwatch.start();
                if (parentID == null) {
                    // if parentID is null, it's a new root in current neuron
                    annotationModel.addRootAnnotation(currentNeuron, finalLocation);
                } else {
                    annotationModel.addChildAnnotation(
                            currentNeuron.getGeoAnnotationMap().get(parentID), finalLocation);
                }
                // stopwatch.stop();
                // System.out.println("added annotation; elapsed time = " + stopwatch.toString());
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
     * delete the annotation with the input ID; the annotation must be a "link",
     * which is an annotation that is not a root (no parent) or branch point
     * (many children); in other words, it's an end point, or an annotation with
     * a parent and single child that can be connected up unambiguously
     */
    public void deleteLink(final Long annotationID) {
        if (annotationModel.getCurrentWorkspace() == null) {
            // dialog?
            return;
        } else {
            // verify it's a link and not a root or branch:
            TmGeoAnnotation annotation = annotationModel.getGeoAnnotationFromID(annotationID);
            if (annotation == null) {
                presentError(
                        "No annotation to delete.",
                        "No such annotation");
            }
            if (annotation.isRoot() || annotation.getChildIds().size() > 1) {
                presentError(
                        "This annotation is either a root (no parent) or branch (many children), not a link!",
                        "Not a link!");
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
     * delete the annotation with the input ID, and delete all of its
     * descendants
     */
    public void deleteSubTree(final Long annotationID) {
        if (annotationModel.getCurrentWorkspace() == null || annotationID == null) {
            // dialog?
            return;
        } else {

            // if more than a handful of nodes, ask the user if they are sure (we have
            //  no undo right now!)
            final TmGeoAnnotation annotation = annotationModel.getGeoAnnotationFromID(annotationID);
            int nAnnotations = annotationModel.getNeuronFromAnnotationID(annotationID).getSubTreeList(annotation).size();
            if (nAnnotations >= 5) {
                int ans = JOptionPane.showConfirmDialog(
                        ComponentUtil.getLVVMainWindow(),
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
                    annotationModel.deleteSubTree(annotation);
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
    public void moveAnnotation(final Long annotationID, final Vec3 micronLocation) {
        if (annotationModel.getCurrentWorkspace() == null) {
            // dialog?
            return;
        } else {
            SimpleWorker mover = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    TileFormat tileFormat = getTileFormat();
                    Vec3 voxelLocation = tileFormat.voxelVec3ForMicronVec3(micronLocation);
                    annotationModel.moveAnnotation(annotationID, voxelLocation);
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

    public boolean canMergeNeurite(Anchor anchor, TmGeoAnnotation closest) {

        // can't merge with itself
        if (anchor.getGuid().equals(closest.getId())) {
            return false;
        }

        // distance: close enough?
        Vec3 anchorLocMicron = anchor.getLocation();
        TileFormat tileFormat = getTileFormat();
        Vec3 anchorLocVox = tileFormat.voxelVec3ForMicronVec3(anchorLocMicron);
        double dx = closest.getX() - anchorLocVox.getX();
        double dy = closest.getY() - anchorLocVox.getY();
        double dz = closest.getZ() - anchorLocVox.getZ();
        if (dx * dx + dy * dy + dz * dz > DRAG_MERGE_THRESHOLD_SQUARED) {
            return false;
        }

        // can't merge with same neurite (don't create cycles!)
        TmGeoAnnotation sourceAnnotation = annotationModel.getGeoAnnotationFromID(anchor.getGuid());
        TmGeoAnnotation targetAnnotation = annotationModel.getGeoAnnotationFromID(closest.getId());
        if (annotationModel.getNeuriteRootAnnotation(sourceAnnotation).getId().equals(
                annotationModel.getNeuriteRootAnnotation(targetAnnotation).getId())) {
            return false;
        }

        return true;
    }

    /**
     * merge the two neurites to which the two annotations belong
     *
     * @param sourceAnnotationID
     * @param targetAnnotationID
     */
    public void mergeNeurite(final Long sourceAnnotationID, final Long targetAnnotationID) {

        TmGeoAnnotation sourceAnnotation = annotationModel.getGeoAnnotationFromID(sourceAnnotationID);
        TmGeoAnnotation targetAnnotation = annotationModel.getGeoAnnotationFromID(targetAnnotationID);
        // System.out.println("merge requested, " + sourceAnnotationID + " to " + targetAnnotationID);

        // same neurite = cycle = NO!
        // this should already be filtered out, but it's important enough to check twice
        if (annotationModel.getNeuriteRootAnnotation(sourceAnnotation).getId().equals(
                annotationModel.getNeuriteRootAnnotation(targetAnnotation).getId())) {
            presentError(
                    "You can't merge a neurite with itself!",
                    "Can't merge!!");
            annotationModel.fireAnnotationNotMoved(sourceAnnotation);
//            annotationModel.annotationNotMovedSignal.emit(sourceAnnotation);
            return;
        }

        // Message goes before title. Why?  
        // Because there are several overrides, all of which use the message as 
        // the first string param. The title is optional. Confusing if not used
        // often, however.
        int ans = JOptionPane.showConfirmDialog(
                ComponentUtil.getLVVMainWindow(),
                String.format("Merge neurite from neuron %s\nto neurite in neuron %s?",
                        annotationModel.getNeuronFromAnnotationID(sourceAnnotationID),
                        annotationModel.getNeuronFromAnnotationID(targetAnnotationID)),
                "Merge neurites?",
                JOptionPane.OK_CANCEL_OPTION);
        if (ans != JOptionPane.OK_OPTION) {
            annotationModel.fireAnnotationNotMoved(sourceAnnotation);
//            annotationModel.annotationNotMovedSignal.emit(sourceAnnotation);
            return;
        }

        // then call ann model
        SimpleWorker merger = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                annotationModel.mergeNeurite(sourceAnnotationID, targetAnnotationID);
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
        merger.execute();

    }

    /**
     * place a new annotation near the annotation with the input ID; place it
     * "nearby" in the direction of its parent if it has one; if it's a root
     * annotation with one child, place it in the direction of the child
     * instead; if it's a root with many children, it's an error, since there is
     * no unambiguous location to place the new anchor
     */
    public void splitAnchor(Long annotationID) {
        if (annotationModel.getCurrentWorkspace() == null) {
            // dialog?
            return;
        }

        // can't split a root if it has multiple children (ambiguous):
        final TmGeoAnnotation annotation = annotationModel.getGeoAnnotationFromID(annotationID);
        if (annotation.isRoot() && annotation.getChildIds().size() != 1) {
            presentError(
                    "Cannot split root annotation with multiple children (ambiguous)!",
                    "Error");
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
                presentError(
                        "Could not split anchor!",
                        "Error");
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
                presentError(
                        "Could not reroot neurite!",
                        "Error");
            }
        };
        rerooter.execute();
    }

    public void splitNeurite(final Long newRootAnnotationID) {
        if (annotationModel.getCurrentWorkspace() == null) {
            return;
        }

        // if it's already the root, can't split
        final TmGeoAnnotation annotation = annotationModel.getGeoAnnotationFromID(newRootAnnotationID);
        if (annotation.isRoot()) {
            presentError(
                    "Cannot split neurite at its root annotation!",
                    "Error");
            return;
        }

        SimpleWorker splitter = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                annotationModel.splitNeurite(annotation.getId());
            }

            @Override
            protected void hadSuccess() {
                // nothing here, model emits signals
            }

            @Override
            protected void hadError(Throwable error) {
                presentError(
                        "Could not split neurite!",
                        "Error");
            }
        };
        splitter.execute();

    }

    /**
     * add an anchored path; not much to check, as the UI needs to check it even
     * before the request gets here
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
                presentError(
                        "Could not add anchored path!",
                        "Error");
            }
        };
        adder.execute();
    }

    /**
     * pop a dialog to add, edit, or delete note at the given annotation
     */
    public void addEditNote(final Long annotationID) {
        TmNeuron neuron = annotationModel.getNeuronFromAnnotationID(annotationID);

        // get annotation if it exists, and its note value, if it exists
        final TmStructuredTextAnnotation textAnnotation = neuron.getStructuredTextAnnotationMap().get(annotationID);
        String noteText = new String("");
        if (textAnnotation != null) {
            JsonNode rootNode = textAnnotation.getData();
            JsonNode noteNode = rootNode.path("note");
            if (!noteNode.isMissingNode()) {
                noteText = noteNode.asText();
            }
        }

        // pop dialog, with (possibly) pre-existing text
        Object[] options = {"Set note",
            "Delete note",
            "Cancel"};
        JPanel panel = new JPanel();
        panel.add(new JLabel("Enter note text:"));
        JTextField textField = new JTextField(40);
        textField.setText(noteText);
        panel.add(textField);
        int ans = JOptionPane.showOptionDialog(
                ComponentUtil.getLVVMainWindow(),
                panel,
                "Add, edit, or delete note",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]);

        if (ans == JOptionPane.CANCEL_OPTION) {
            return;
        } else if (ans == JOptionPane.NO_OPTION) {
            // no option = delete note, which we signal by empty note text
            noteText = "";
        } else {
            noteText = textField.getText().trim();
        }

        if (noteText.length() > 0) {

            final String setText = noteText;
            SimpleWorker setter = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    annotationModel.setNote(annotationModel.getGeoAnnotationFromID(annotationID), setText);
                }

                @Override
                protected void hadSuccess() {
                    // nothing here
                }

                @Override
                protected void hadError(Throwable error) {
                    presentError(
                            "Could not set note!",
                            "Error");
                }
            };
            setter.execute();

        } else {
            if (textAnnotation != null) {
                SimpleWorker deleter = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        annotationModel.removeNote(textAnnotation);
                    }

                    @Override
                    protected void hadSuccess() {
                        // nothing to see
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        presentError(
                                "Could not remove note!",
                                "Error");
                    }
                };
                deleter.execute();
            }
        }

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
                // Renaming neuron here, after selection is done.
                renameNeuron();
            }

            @Override
            protected void hadSuccess() {
                // nothing here, annModel emits its own signals
            }

            @Override
            protected void hadError(Throwable error) {
                presentError(
                        "Could not create neuron!",
                        "Error");
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
        int ans = JOptionPane.showConfirmDialog(
                ComponentUtil.getLVVMainWindow(),
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
                    presentError(
                            "Could not delete current neuron!",
                            "Error");
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
            presentError(
                    "No selected neuron!",
                    "No neuron!");
            return;
        }

        final String neuronName = (String) JOptionPane.showInputDialog(
                ComponentUtil.getLVVMainWindow(),
                "Neuron name:",
                "Rename neuron",
                JOptionPane.PLAIN_MESSAGE,
                null, // icon
                null, // choice list; absent = freeform
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
                presentError(
                        "Could not rename neuron!",
                        "Error");
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
        Pattern pattern = Pattern.compile("New[ _]neuron[ _]([0-9]+)");
        for (TmNeuron neuron : workspace.getNeuronList()) {
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
            for (Long l : intList) {
                if (l > maximum) {
                    maximum = l;
                }
            }
        }
        return String.format("New_neuron_%d", maximum + 1);
    }

    /**
     * given an annotation ID, select (make current) the neuron it belongs to
     */
    public void selectNeuronFromAnnotation(Long annotationID) {
        if (annotationID == null) {
            return;
        }

        TmNeuron neuron = annotationModel.getNeuronFromAnnotationID(annotationID);
        annotationModel.selectNeuron(neuron);
    }

    /**
     * create a new workspace with the currently loaded sample
     */
    public void createWorkspace() {
        // if no sample loaded, error
        if (initialEntity == null) {
            presentError(
                    "You must load a brain sample entity before creating a workspace!",
                    "No brain sample!");
            return;
        }

        // check that the entity *is* a workspace or brain sample, and get the sample ID:
        Long sampleID;
        if (initialEntity.getEntityTypeName().equals(EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE)) {
            sampleID = initialEntity.getId();
        } else if (initialEntity.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
            sampleID = annotationModel.getCurrentWorkspace().getSampleID();
        } else {
            presentError(
                    "You must load a brain sample before creating a workspace!",
                    "No brain sample!");
            return;
        }

        // ask the user if they really want a new workspace if one is active
        final boolean existingWorkspace = annotationModel.getCurrentWorkspace() != null;
        if (existingWorkspace) {
            int ans = JOptionPane.showConfirmDialog(
                    ComponentUtil.getLVVMainWindow(),
                    "You already have an active workspace!  Close and create another?",
                    "Workspace exists",
                    JOptionPane.YES_NO_OPTION);
            if (ans == JOptionPane.NO_OPTION) {
                return;
            }
        }

        // get a name for the new workspace and validate (are there any rules for entity names?)
        String workspaceName = (String) JOptionPane.showInputDialog(
                ComponentUtil.getLVVMainWindow(),
                "Workspace name:",
                "Create workspace",
                JOptionPane.PLAIN_MESSAGE,
                null, // icon
                null, // choice list; absent = freeform
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

                // and if there was a previously existing workspace, we'll save the
                //  existing color model (which isn't cleared by creating a new
                //  workspace) into the new workspace
                if (existingWorkspace) {
                    saveColorModel();
                }
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
        savePreference(AnnotationsConstants.PREF_COLOR_MODEL, quadViewUi.imageColorModelAsString());
    }

    public void setAutomaticRefinement(final boolean state) {
        savePreference(AnnotationsConstants.PREF_AUTOMATIC_POINT_REFINEMENT, String.valueOf(state));
    }

    public void setAutomaticTracing(final boolean state) {
        savePreference(AnnotationsConstants.PREF_AUTOMATIC_TRACING, String.valueOf(state));
    }

    public void savePreference(final String name, final String value) {
        SimpleWorker saver = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                annotationModel.setPreference(name, value);
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

    public String retreivePreference(final String name) {
        return annotationModel.getPreference(name);
    }

    private void tracePathToParent(PathTraceToParentRequest request) {
        TmGeoAnnotation annotation = annotationModel.getGeoAnnotationFromID(request.getAnchorGuid1());
        if (annotation.isRoot()) {
            // no parent, no tracing
            return;
        }

        TmNeuron neuron = annotationModel.getNeuronFromAnnotationID(annotation.getId());
        TmGeoAnnotation parent = neuron.getParentOf(annotation);
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

    public void exportAllNeuronsAsSWC(final File swcFile, final int downsampleModulo) {
        final List<Long> neuronIDList = new ArrayList<>();
        int nannotations = 0;
        for (TmNeuron neuron : annotationModel.getCurrentWorkspace().getNeuronList()) {
            nannotations += neuron.getGeoAnnotationMap().size();
            neuronIDList.add(neuron.getId());
        }
        if (nannotations == 0) {
            presentError("No points in any neuron!", "Export error");
        }

        SimpleWorker saver = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                annotationModel.exportSWCData(swcFile, neuronIDList, downsampleModulo);
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

    public void exportCurrentNeuronAsSWC(final File swcFile, final int downsampleModulo) {
        if (annotationModel.getCurrentNeuron().getGeoAnnotationMap().size() == 0) {
            presentError("Neuron has no points!", "Export error");
        }

        final Long neuronID = annotationModel.getCurrentNeuron().getId();
        SimpleWorker saver = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                annotationModel.exportSWCData(swcFile, Arrays.asList(neuronID), downsampleModulo);
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

    public void importSWCFile(final File swcFile) {
        if (annotationModel.getCurrentWorkspace() == null) {
            // dialog?
            return;
        }

        if (!swcFile.exists()) {
            presentError(
                    "SWC file " + swcFile.getName() + " does not exist!",
                    "No SWC file!");
        } else {

            // note for the future: at this point, we could pop another dialog with:
            //  (a) info: file has xxx nodes; continue?
            //  (b) option to downsample
            //  (c) option to include/exclude automatically traced paths, if we can
            //      store that info in the file
            //  (d) option to shift position (add constant x, y, z offset)
            BackgroundWorker importer = new BackgroundWorker() {
                @Override
                protected void doStuff() throws Exception {
                    annotationModel.importSWCData(swcFile, this);
                }

                @Override
                public String getName() {
                    return "import " + swcFile.getName();
                }

                @Override
                protected void hadSuccess() {
                    // signal will be sent, stuff will happen...
                }

                @Override
                protected void hadError(Throwable error) {
                    SessionMgr.getSessionMgr().handleException(error);
                }
            };
            importer.executeWithEvents();
        }
    }

    /**
     * Convenience method, to cut down on redundant code.
     *
     * @param message passed as message param
     * @param title passed as title param.
     * @throws HeadlessException by called methods.
     */
    public void presentError(String message, String title) throws HeadlessException {
        JOptionPane.showMessageDialog(
                ComponentUtil.getLVVMainWindow(),
                message,
                title,
                JOptionPane.ERROR_MESSAGE);
    }

}
