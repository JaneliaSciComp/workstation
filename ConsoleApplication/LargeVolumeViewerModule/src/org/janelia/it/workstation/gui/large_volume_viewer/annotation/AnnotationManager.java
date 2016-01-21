package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import com.google.common.base.Stopwatch;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.large_volume_viewer.QuadViewUi;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Anchor;

import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyle;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyleDialog;
import org.janelia.it.workstation.shared.workers.BackgroundWorker;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.workstation.tracing.AnchoredVoxelPath;
import org.janelia.it.workstation.tracing.PathTraceToParentRequest;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;
import org.janelia.it.workstation.tracing.PathTraceToParentWorker;

import org.janelia.it.workstation.gui.large_volume_viewer.ComponentUtil;
import org.janelia.it.workstation.gui.large_volume_viewer.TileFormat;
import org.janelia.it.workstation.gui.large_volume_viewer.TileServer;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.UpdateAnchorListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.PathTraceListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.VolumeLoadListener;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Skeleton.AnchorSeed;
import org.janelia.it.workstation.tracing.VoxelPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.janelia.it.workstation.gui.large_volume_viewer.activity_logging.ActivityLogHelper;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;

public class AnnotationManager implements UpdateAnchorListener, PathTraceListener, VolumeLoadListener
/**
 * this class is the middleman between the UI and the model. first, the UI makes
 * naive requests (eg, add annotation). then this class determines if the
 * request is valid (eg, can't add if no neuron), popping dialogs if needed.
 * lastly, this class gathers and/or reformats info as needed to actually make
 * the call to the back end, usually spinning off a worker thread to do so.
 *
 * this class's events are usually connected to various UI signals, and it
 * typically fires events for AnnotationModel. this class has no
 * responsibilities in notifying UI elements of what's been done; that's handled
 * by events generated at AnnotationModel.
 */
{

    ModelMgr modelMgr;

    private static final Logger log = LoggerFactory.getLogger(AnnotationManager.class);

    // annotation model object
    private AnnotationModel annotationModel;
    
    private ActivityLogHelper activityLog;
    
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

    public AnnotationManager(AnnotationModel annotationModel, QuadViewUi quadViewUi, TileServer tileServer) {
        this.annotationModel = annotationModel;
        this.quadViewUi = quadViewUi;
        this.tileServer = tileServer;
        modelMgr = ModelMgr.getModelMgr();
        activityLog = new ActivityLogHelper();
    }

    public void deleteSubtreeRequested(Anchor anchor) {
        if (anchor != null) {
            deleteSubTree(anchor.getGuid());
        } else {
            int x = 0;
        }
    }

    public void splitAnchorRequested(Anchor anchor) {
        splitAnchor(anchor.getGuid());
    }

    public void rerootNeuriteRequested(Anchor anchor) {
        rerootNeurite(anchor.getGuid());
    }

    public void splitNeuriteRequested(Anchor anchor) {
        splitNeurite(anchor.getGuid());
    }

    public void deleteLinkRequested(Anchor anchor) {
        deleteLink(anchor.getGuid());
    }

    public void addEditNoteRequested(Anchor anchor) {
        if (anchor != null) {
            addEditNote(anchor.getGuid());
        }
    }

    public void anchorAdded(AnchorSeed seed) {
        addAnnotation(seed.getLocation(), seed.getParentGuid());
    }

    public void moveAnchor(Anchor anchor) {
        // find closest to new anchor location that isn't the annotation already
        //  associated with anchor; remember that anchors are in micron
        //  coords, and we need voxels!
        TileFormat.VoxelXyz tempLocation = getTileFormat().voxelXyzForMicrometerXyz(
                new TileFormat.MicrometerXyz(anchor.getLocation().getX(),
                        anchor.getLocation().getY(),anchor.getLocation().getZ()));
        Vec3 anchorVoxelLocation = new Vec3(tempLocation.getX(),
                tempLocation.getY(), tempLocation.getZ());

        TmGeoAnnotation closest = annotationModel.getClosestAnnotation(anchorVoxelLocation,
                annotationModel.getGeoAnnotationFromID(anchor.getGuid()));

        // check distance and other restrictions
        if (closest != null && canMergeNeurite(anchor.getGuid(), anchorVoxelLocation, closest.getId())) {
            // check if user wants to merge (expensive to undo) or move (near something that
            //  is valid to merge with), or nothing (undo drag)
            Object[] options = {"Merge", "Move, don't merge", "Cancel"};
            int ans = JOptionPane.showOptionDialog(
                    ComponentUtil.getLVVMainWindow(),
                    String.format("Merge neurite from neuron %s\nto neurite in neuron %s?",
                            annotationModel.getNeuronFromAnnotationID(anchor.getGuid()),
                            annotationModel.getNeuronFromAnnotationID(closest.getId())),
                    "Merge neurites?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[2]
            );
            if (ans == JOptionPane.CANCEL_OPTION) {
                annotationModel.fireAnnotationNotMoved(annotationModel.getGeoAnnotationFromID(anchor.getGuid()));
                return;
            } else if (ans == JOptionPane.YES_OPTION) {
                activityLog.logMergedNeurite(getSampleID(), closest);
                mergeNeurite(anchor.getGuid(), closest.getId());
            } else {
                // move, don't merge
                activityLog.logMergedNeurite(getSampleID(), closest);
                moveAnnotation(anchor.getGuid(), anchorVoxelLocation);
            }
        } else {
            moveAnnotation(anchor.getGuid(), anchorVoxelLocation);
        }
    }

    //-----------------------------IMPLEMENT UpdateAnchorListener
    @Override
    public void update(Anchor anchor) {
        if (anchor != null) {
            selectNeuronFromAnnotation(anchor.getGuid());
        }
    }

    //-----------------------------IMPLEMENTS PathTraceListener
    @Override
    public void pathTraced(AnchoredVoxelPath voxelPath) {
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

    //-------------------------------IMPLEMENTS VolumeLoadListener
    @Override
    public void volumeLoaded(URL url) {
        onVolumeLoaded();
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
            final ProgressHandle progress = ProgressHandleFactory.createHandle("Loading annotations...");
            SimpleWorker loader = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    // Must make known to user that things are happening,
                    // even if slowly.
                    progress.start();
                    progress.setDisplayName("Loading annotations");
                    progress.switchToIndeterminate();
                    TmWorkspace workspace = modelMgr.loadWorkspace(initialEntity.getId());
                    // at this point, we know the entity is a workspace, so:
                    annotationModel.loadWorkspace(workspace);
                }

                @Override
                protected void hadSuccess() {
                    // no hadSuccess(); signals will be emitted in the loadWorkspace() call
                    progress.finish();
                }

                @Override
                protected void hadError(Throwable error) {
                    progress.finish();
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
                    Stopwatch stopwatch = new Stopwatch();
                    stopwatch.start();
                    PointRefiner refiner = new PointRefiner(quadViewUi.getSubvolumeProvider());
                    finalLocation = refiner.refine(xyz);
                    stopwatch.stop();
                    // System.out.println("refined annotation; elapsed time = " + stopwatch.toString());
                    log.info("refined annotation; elapsed time = " + stopwatch);

                    // System.out.println("add annotation: input point " + xyz);
                    // System.out.println("add annotation: refined point " + finalLocation);
                } else {
                    finalLocation = xyz;
                }

                Stopwatch stopwatch = new Stopwatch();
                stopwatch.start();
                activityLog.logAddAnchor(AnnotationManager.this.annotationModel.getCurrentWorkspace().getSampleID(), finalLocation);
                if (parentID == null) {
                    // if parentID is null, it's a new root in current neuron
                    annotationModel.addRootAnnotation(currentNeuron, finalLocation);
                } else {
                    annotationModel.addChildAnnotation(
                            currentNeuron.getGeoAnnotationMap().get(parentID), finalLocation);
                }
                stopwatch.stop();
                // System.out.println("added annotation; elapsed time = " + stopwatch.toString());
                log.info("added annotation; elapsed time = " + stopwatch);
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
            final TmGeoAnnotation annotation = annotationModel.getGeoAnnotationFromID(annotationID);
            if (annotation == null) {
                presentError(
                        "No annotation to delete.",
                        "No such annotation");
            }
            if (annotation.isRoot() && annotation.getChildIds().size() > 0) {
                presentError(
                        "This annotation is a root with children, not a link!",
                        "Not a link!");
                return;
            }
            if (annotation.getChildIds().size() > 1) {
                presentError(
                        "This annotation is a branch (many children), not a link!",
                        "Not a link!");
                return;
            }

            SimpleWorker deleter = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    activityLog.logDeleteLink(getSampleID(), annotation);
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
            activityLog.logDeleteSubTree(getSampleID(), annotation);
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
                    annotationModel.moveAnnotation(annotationID, micronLocation);
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

    public boolean canMergeNeurite(Long anchorID, Vec3 anchorLocation, Long annotationID) {

        // can't merge with itself
        if (anchorID.equals(annotationID)) {
            return false;
        }

        TmGeoAnnotation sourceAnnotation = annotationModel.getGeoAnnotationFromID(anchorID);
        TmGeoAnnotation targetAnnotation = annotationModel.getGeoAnnotationFromID(annotationID);

        // is the target neuron visible?
        if (!getNeuronStyle(annotationModel.getNeuronFromNeuronID(targetAnnotation.getNeuronId())).isVisible()) {
            return false;
        }

        // distance: close enough?
        double dx = anchorLocation.getX() - targetAnnotation.getX();
        double dy = anchorLocation.getY() - targetAnnotation.getY();
        double dz = anchorLocation.getZ() - targetAnnotation.getZ();
        if (dx * dx + dy * dy + dz * dz > DRAG_MERGE_THRESHOLD_SQUARED) {
            return false;
        }

        // can't merge with same neurite (don't create cycles!)
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


    public void moveNeuriteRequested(Anchor anchor) {
        if (anchor == null) {
            presentError("Anchor unexpectedly null!", "Can't move neurite");
            return;
        }

        // dialog box with list of neurons, not including current neuron; but
        //  throw in a dummy "create new neuron" option at the top
        final TmGeoAnnotation annotation = annotationModel.getGeoAnnotationFromID(anchor.getGuid());
        TmNeuron sourceNeuron = annotationModel.getNeuronFromAnnotationID(annotation.getId());

        ArrayList<TmNeuron> neuronList = new ArrayList<>(annotationModel.getCurrentWorkspace().getNeuronList());
        neuronList.remove(sourceNeuron);
        // not sure alphabetical is the best sort; neuron list is selectable (defaults to creation
        //  date), but I don't want to figure out how to grab that sort order and use it here;
        //  however, alphabetical seems reasonable enough (better than arbitrary order)
        Collections.sort(neuronList, new Comparator<TmNeuron>() {
            @Override
            public int compare(TmNeuron tmNeuron, TmNeuron tmNeuron2) {
                return tmNeuron.getName().compareToIgnoreCase(tmNeuron2.getName());
            }
        });

        // add "create new" at top of sorted list
        TmNeuron dummyCreateNewNeuron = new TmNeuron(-1L, "(create new neuron)");
        neuronList.add(0, dummyCreateNewNeuron);

        Object [] choices = neuronList.toArray();
        Object choice = JOptionPane.showInputDialog(
                ComponentUtil.getLVVMainWindow(),
                "Choose destination neuron:",
                "Choose neuron",
                JOptionPane.QUESTION_MESSAGE,
                null,
                choices,
                choices[0]
        );
        if (choice == null) {
            return;
        }

        if (((TmNeuron) choice).getId().equals(dummyCreateNewNeuron.getId())) {
            // create new neuron and move neurite to it
            final String neuronName = promptForNeuronName(null);
            if (neuronName == null) {
                JOptionPane.showMessageDialog(
                    ComponentUtil.getLVVMainWindow(),
                    "Neuron rename canceled; move neurite canceled",
                    "Move neurite canceled",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            SimpleWorker mover = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    // need to create neuron, then compare new neuron list to old one,
                    //  so we can figure out what the new one is, since create neuron
                    //  can't tell us
                    Set<Long> oldNeuronIDs = new HashSet<>();
                    for (TmNeuron neuron: annotationModel.getCurrentWorkspace().getNeuronList()) {
                        oldNeuronIDs.add(neuron.getId());
                    }

                    annotationModel.createNeuron(neuronName);

                    TmNeuron newNeuron = null;
                    for (TmNeuron neuron: annotationModel.getCurrentWorkspace().getNeuronList()) {
                        if (!oldNeuronIDs.contains(neuron.getId())) {
                            newNeuron = neuron;
                            break;
                        }
                    }

                    annotationModel.moveNeurite(annotation, newNeuron);
                }

                @Override
                protected void hadSuccess() {
                    // nothing to see here
                }

                @Override
                protected void hadError(Throwable error) {
                    presentError(
                            "Error while moving neurite!",
                            "Error",
                            error);
                }
            };
            mover.execute();

        } else {
            // we're moving to an existing neuron; straightforward!
            final TmNeuron destinationNeuron = (TmNeuron) choice;
            SimpleWorker mover = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    annotationModel.moveNeurite(annotation, destinationNeuron);
                }

                @Override
                protected void hadSuccess() {
                    // nothing to see here
                }

                @Override
                protected void hadError(Throwable error) {
                    presentError(
                            "Error while moving neurite!",
                            "Error",
                            error);
                }
            };
            mover.execute();
        }

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
                activityLog.logSplitAnnotation(getSampleID(), annotation);
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
                        "Error",
                        error);
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
                activityLog.logRerootNeurite(getSampleID(), newRootAnnotationID);
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
                        "Error",
                        error);
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
                        "Error",
                        error);
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
                        "Error",
                        error);
            }
        };
        adder.execute();
    }

    /**
     * pop a dialog to add, edit, or delete note at the given annotation
     */
    public void addEditNote(final Long annotationID) {
        String noteText = getNote(annotationID);

        AddEditNoteDialog testDialog = new AddEditNoteDialog(
            (Frame) SwingUtilities.windowForComponent(ComponentUtil.getLVVMainWindow()),
            noteText,
            annotationModel.getNeuronFromAnnotationID(annotationID),
            annotationID);
        testDialog.setVisible(true);
        if (testDialog.isSuccess()) {
            String resultText = testDialog.getOutputText().trim();
            if (resultText.length() > 0) {
                setNote(annotationID, resultText);
            } else {
                // empty string means delete note
                clearNote(annotationID);
            }
        } else {
            // canceled
            return;
        }
    }

    public void clearNote(Long annotationID) {
        TmNeuron neuron = annotationModel.getNeuronFromAnnotationID(annotationID);
        final TmStructuredTextAnnotation textAnnotation = neuron.getStructuredTextAnnotationMap().get(annotationID);
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
                            "Error",
                            error);
                }
            };
            deleter.execute();
        }
    }

    /**
     * returns the note attached to a given annotation; returns empty
     * string if there is no note; you'll get an exception if the
     * annotation ID doesn't exist
     */
    public String getNote(Long annotationID) {
        return annotationModel.getNote(annotationID);
    }

    public String getNote(Long annotationID, TmNeuron neuron) {
        return annotationModel.getNote(annotationID, neuron);
    }

    public void setNote(final Long annotationID, final String noteText) {
        SimpleWorker setter = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                annotationModel.setNote(annotationModel.getGeoAnnotationFromID(annotationID), noteText);
            }

            @Override
            protected void hadSuccess() {
                // nothing here
            }

            @Override
            protected void hadError(Throwable error) {
                presentError(
                        "Could not set note!",
                        "Error",
                        error);
            }
        };
        setter.execute();
    }

    /**
     * create a new neuron in the current workspace, prompting for name
     */
    public void createNeuron() {
        if (annotationModel.getCurrentWorkspace() == null) {
            // dialog?
            return;
        }

        // prompt the user for a name, but suggest a standard name
        final String neuronName = promptForNeuronName(getNeuronName(annotationModel.getCurrentWorkspace()));

        if (neuronName != null) {
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
                    presentError(
                            "Could not create neuron!",
                            "Error",
                            error);
                }
            };
            creator.execute();
        }
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
                            "Error",
                            error);
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

        final String neuronName = promptForNeuronName(neuron.getName());
        if (neuronName == null) {
            return;
        }

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
                        "Error",
                        error);
            }
        };
        renamer.execute();

    }


    /**
     * pop a dialog that asks for a name for a neuron;
     * returns null if the user didn't make a choice
     */
    String promptForNeuronName(String suggestedName) {
        if (suggestedName == null) {
            suggestedName = "";
        }
        String neuronName = (String) JOptionPane.showInputDialog(
                ComponentUtil.getLVVMainWindow(),
                "Neuron name:",
                "Name neuron",
                JOptionPane.PLAIN_MESSAGE,
                null, // icon
                null, // choice list; absent = freeform
                suggestedName);
        if (neuronName == null || neuronName.length() == 0) {
            return null;
        } else {
            // if we had any validation to do, we'd do it
            // here...but we don't
            return neuronName;
        }
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
        Pattern pattern = Pattern.compile("Neuron[ _]([0-9]+)");
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
        return String.format("Neuron %d", maximum + 1);
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
            sampleID = getSampleID();
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


    /**
     * find an annotation relative to the input annotation in
     * a given direction; to be used in navigation along the skeleton;
     * see code for exact behavior
     */
    public Long relativeAnnotation(Long annID, TmNeuron.AnnotationNavigationDirection direction) {
        TmGeoAnnotation ann = annotationModel.getGeoAnnotationFromID(annID);
        TmNeuron neuron = annotationModel.getNeuronFromAnnotationID(annID);
        switch (direction) {
            case ROOTWARD_JUMP:
            case ROOTWARD_STEP:
                // if root, done; anything else, move to next rootward branch,
                //  or root if there isn't one
                if (ann.isRoot()) {
                    break;
                }
                ann = neuron.getParentOf(ann);
                if (direction == TmNeuron.AnnotationNavigationDirection.ROOTWARD_STEP) {
                    break;
                }
                while (!ann.isRoot() && !ann.isBranch()) {
                    ann = neuron.getParentOf(ann);
                }
                break;
            case ENDWARD_JUMP:
            case ENDWARD_STEP:
                // if no children (already end), done; if straight, move through
                //  children until branch or end; if branch, take first child and
                //  move to branch or end
                if (ann.isEnd()) {
                    break;
                }
                ann = neuron.getChildrenOfOrdered(ann).get(0);
                if (direction == TmNeuron.AnnotationNavigationDirection.ENDWARD_STEP) {
                    break;
                }
                while (!ann.isEnd() && !ann.isBranch()) {
                    ann = neuron.getChildrenOf(ann).get(0);
                }
                break;
            case NEXT_PARALLEL:
            case PREV_PARALLEL:
                // easy case first: on root with one child, nothing
                if (ann.isRoot() && neuron.getChildrenOf(ann).size() == 1) {
                    break;
                }

                //  on annotation descendant of root with no branches, nothing
                //  on annotation that has a rootward branch point: move to first
                //      link after next child of that branch
                // find the next branch/root up, and keep track of the first child after the branch
                TmGeoAnnotation previous = ann;
                TmGeoAnnotation current= neuron.getParentOf(previous);
                while (!current.isBranch() && !current.isRoot()) {
                    previous = current;
                    current = neuron.getParentOf(previous);
                }

                // is it the root with one child?
                if (current.isRoot() && neuron.getChildrenOf(current).size() == 1) {
                    break;
                }

                // now we're on a branch (possibly the root) that has multiple children;
                //  find the next after the one we've got
                List<TmGeoAnnotation> children = neuron.getChildrenOfOrdered(current);
                int offset = (direction == TmNeuron.AnnotationNavigationDirection.NEXT_PARALLEL) ? 1 : -1;
                Collections.rotate(children, -children.indexOf(previous) + offset);
                ann = children.get(0);
                break;
        }

        return ann.getId();
    }


    /**
     * pop a dialog to choose neuron style; three variants work together to operate
     * from different input sources
     */
    public void chooseNeuronStyle() {
        if (annotationModel.getCurrentWorkspace() == null) {
            return;
        }
        if (annotationModel.getCurrentNeuron() == null) {
            presentError("You must select a neuron to set its style.", "No neuron selected");
        } else {
            chooseNeuronStyle(annotationModel.getCurrentNeuron());
        }
    }

    public void chooseNeuronStyle(Anchor anchor) {
        chooseNeuronStyle(annotationModel.getNeuronFromAnnotationID(anchor.getGuid()));
    }

    public void chooseNeuronStyle(final TmNeuron neuron) {
        if (annotationModel.getCurrentWorkspace() == null) {
            return;
        }
        if (neuron == null) {
            // should not happen
            return;
        }

        NeuronStyleDialog dialog = new NeuronStyleDialog(
                (Frame) SwingUtilities.windowForComponent(ComponentUtil.getLVVMainWindow()),
                getNeuronStyle(neuron));
        dialog.setVisible(true);
        if (dialog.styleChosen()) {
            setNeuronStyle(neuron, dialog.getChosenStyle());
        }
    }

    public void setAllNeuronVisibility(boolean visibility) {
        // feels like I should get the full NeuronStyle map here, but then I'd
        //  have to worry about missing entries, etc.; so go simple at the
        //  expense of a few more db calls
        NeuronStyle style;
        for (TmNeuron neuron: annotationModel.getCurrentWorkspace().getNeuronList()) {
            style = getNeuronStyle(neuron);
            if (style.isVisible() != visibility) {
                style.setVisible(visibility);
                setNeuronStyle(neuron, style);
            }
        }
    }

    /**
     * as with chooseNeuronStyle, multiple versions allow for multiple entry points
     */
    public void setNeuronVisibility(boolean visibility) {
        if (annotationModel.getCurrentWorkspace() == null) {
            return;
        }
        if (annotationModel.getCurrentNeuron() == null) {
            presentError("You must select a neuron to hide or show it.", "No neuron selected");
        } else {
            setNeuronVisibility(annotationModel.getCurrentNeuron(), visibility);
        }

    }

    public void setNeuronVisibility(Anchor anchor, boolean visibility) {
        TmNeuron neuron = annotationModel.getNeuronFromAnnotationID(anchor.getGuid());
        setNeuronVisibility(neuron, visibility);
    }

    public void setNeuronVisibility(TmNeuron neuron, boolean visibility) {
        NeuronStyle style = getNeuronStyle(neuron);
        style.setVisible(visibility);
        setNeuronStyle(neuron, style);
    }

    public NeuronStyle getNeuronStyle(TmNeuron neuron) {
        // simple pass through; I want get/set to look the same, and set is *not*
        //  just a pass through
        return annotationModel.getNeuronStyle(neuron);
    }

    public void setNeuronStyle(final TmNeuron neuron, final NeuronStyle style) {
        SimpleWorker setter = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                annotationModel.setNeuronStyle(neuron, style);
            }

            @Override
            protected void hadSuccess() {
                // nothing; listeners will update
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        setter.execute();
    }

    public void saveColorModel() {
        if (annotationModel.getCurrentWorkspace() == null) {
            presentError("You must create a workspace to be able to save the color model!", "No workspace");
        } else {
            savePreference(AnnotationsConstants.PREF_COLOR_MODEL, quadViewUi.imageColorModelAsString());
        }
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

    public String retrievePreference(final String name) {
        return annotationModel.getPreference(name);
    }

    public void tracePathToParent(PathTraceToParentRequest request) {
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
        worker.setPathTraceListener(this);
        worker.execute();

        // we'd really prefer to see this worker's status in the Progress Monitor, but as of
        //  Jan. 2014, that monitor's window repositions itself and comes to front on every
        //  new task, so it's far too intrusive to be used for our purpose; see FW-2191
        // worker.executeWithEvents();
    }

    public void showWorkspaceInfoDialog() {
        // implementation note; this is sloppy; in Raveler, I have a SessionInfoDialog class
        //  that has a nice table layout and methods for adding new lines to that table
        //  (including using blank lines as dividers); next time we need to add info
        //  to this dialog, refactor and do it right

        // you can also look to the Raveler dialog to see what kinds of info we could add
        //  eg,  move sample name, path to here

        if (annotationModel.getCurrentWorkspace() != null) {
            int nneurons = annotationModel.getCurrentWorkspace().getNeuronList().size();
            int nannotations = 0;
            for (TmNeuron neuron : annotationModel.getCurrentWorkspace().getNeuronList()) {
                nannotations += neuron.getGeoAnnotationMap().size();
            }
            JOptionPane.showMessageDialog(quadViewUi,
                    "# neurons = " + nneurons + "\n# annotations (total) = " + nannotations + "\n",
                    "Info",
                    JOptionPane.PLAIN_MESSAGE);
        }
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

    public void importSWCFile(final File swcFile, final AtomicInteger countDownSemaphor) {
        if (annotationModel.getCurrentWorkspace() == null) {
            JOptionPane.showMessageDialog(quadViewUi, "No workspace is open", "Cannot Import", JOptionPane.ERROR_MESSAGE);
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
            if (countDownSemaphor == null) {
                BackgroundWorker importer = new BackgroundWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        annotationModel.importBulkSWCData(swcFile, null, true);
                    }

                    @Override
                    public String getName() {
                        return "import " + swcFile.getName();
                    }

                    @Override
                    protected void hadSuccess() {
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };
                importer.executeWithEvents();
            }
            else {
                SimpleWorker importer = new SimpleWorker() {

                    @Override
                    protected void doStuff() throws Exception {
                        annotationModel.importBulkSWCData(swcFile, null, false);
                    }

                    @Override
                    protected void hadSuccess() {
                        int latestValue = countDownSemaphor.decrementAndGet();
                        if (latestValue == 0) {
                            annotationModel.postWorkspaceUpdate();
                        }
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                    
                };
                importer.execute();
            }
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

    /**
     * usual error dialog is hiding too much info when it pops within
     * a SimpleWorker's error clause; log those errors!
     */
    public void presentError(String message, String title, Throwable error) throws HeadlessException {
        log.error(message, error);
        presentError(message, title);
    }

    private Long getSampleID() {
        return annotationModel.getCurrentWorkspace().getSampleID();
    }

}
