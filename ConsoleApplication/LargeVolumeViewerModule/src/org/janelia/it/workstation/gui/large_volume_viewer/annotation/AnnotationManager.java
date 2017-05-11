package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import java.awt.Color;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.janelia.console.viewerapi.model.ImageColorModel;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.tiledMicroscope.AnnotationNavigationDirection;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmAnchoredPathEndpoints;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmColorModel;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmStructuredTextAnnotation;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.jacs.shared.lvv.TileFormat;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.gui.support.DesktopApi;
import org.janelia.it.workstation.browser.workers.BackgroundWorker;
import org.janelia.it.workstation.browser.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.browser.workers.SimpleListenableFuture;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.it.workstation.gui.large_volume_viewer.ComponentUtil;
import org.janelia.it.workstation.gui.large_volume_viewer.QuadViewUi;
import org.janelia.it.workstation.gui.large_volume_viewer.TileServer;
import org.janelia.it.workstation.gui.large_volume_viewer.action.NeuronTagsAction;
import org.janelia.it.workstation.gui.large_volume_viewer.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.gui.large_volume_viewer.api.ModelTranslation;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.PathTraceListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.UpdateAnchorListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.VolumeLoadListener;
import org.janelia.it.workstation.gui.large_volume_viewer.dialogs.EditWorkspaceNameDialog;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Anchor;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Skeleton.AnchorSeed;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronColorDialog;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyle;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
import org.janelia.it.workstation.tracing.AnchoredVoxelPath;
import org.janelia.it.workstation.tracing.PathTraceToParentRequest;
import org.janelia.it.workstation.tracing.PathTraceToParentWorker;
import org.janelia.it.workstation.tracing.VoxelPosition;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger log = LoggerFactory.getLogger(AnnotationManager.class);

    // annotation model object
    private AnnotationModel annotationModel;

    private ActivityLogHelper activityLog = ActivityLogHelper.getInstance();

    // quad view ui object
    private QuadViewUi quadViewUi;

    private DomainObject initialObject;

    private TileServer tileServer;

    private LargeVolumeViewerTranslator lvvTranslator;

    // ----- constants
    // AUTOMATIC_TRACING_TIMEOUT for automatic tracing in seconds
    private static final double AUTOMATIC_TRACING_TIMEOUT = 10.0;

    // when dragging to merge, how close in pixels (squared) to trigger
    //  a merge instead of a move
    // this distance chosen by trial and error; I annotated a neuron
    //  at what seemed like a reasonable zoom level, and I experimented
    //  until the distance threshold seemed right
    private static final double DRAG_MERGE_THRESHOLD_SQUARED = 250.0;

    public AnnotationManager(AnnotationModel annotationModel, QuadViewUi quadViewUi,
        LargeVolumeViewerTranslator lvvTranslator, TileServer tileServer) {
        this.annotationModel = annotationModel;
        this.quadViewUi = quadViewUi;
        this.tileServer = tileServer;
        this.lvvTranslator = lvvTranslator;
    }

    public boolean editsAllowed() {
        return annotationModel.editsAllowed();
    }
    
    public void deleteSubtreeRequested(Anchor anchor) {
        if (anchor != null) {
            deleteSubTree(anchor.getNeuronID(), anchor.getGuid());
        } 
    }

    public void splitAnchorRequested(Anchor anchor) {
        splitAnchor(anchor.getNeuronID(), anchor.getGuid());
    }

    public void rerootNeuriteRequested(Anchor anchor) {
        rerootNeurite(anchor.getNeuronID(), anchor.getGuid());
    }

    public void splitNeuriteRequested(Anchor anchor) {
        splitNeurite(anchor.getNeuronID(), anchor.getGuid());
    }

    public void deleteLinkRequested(Anchor anchor) {
        deleteLink(anchor.getNeuronID(), anchor.getGuid());
    }

    public void addEditNoteRequested(Anchor anchor) {
        if (anchor != null) {
            addEditNote(anchor.getNeuronID(), anchor.getGuid());
        }
    }

    public void editNeuronTagsRequested(Anchor anchor) {
        if (anchor != null) {
            editNeuronTags(annotationModel.getNeuronFromNeuronID(anchor.getNeuronID()));
        }
    }

    public void anchorAdded(AnchorSeed seed) {
        addAnnotation(seed.getLocation(), seed.getParentGuid());
    }
    
    public void moveAnchor(Anchor anchor) {

        if (!editsAllowed()) {
            JOptionPane.showMessageDialog(FrameworkImplProvider.getMainFrame(), "Workspace is read-only", "Cannot edit annotation", JOptionPane.INFORMATION_MESSAGE);
            annotationModel.fireAnnotationNotMoved(annotationModel.getGeoAnnotationFromID(anchor.getNeuronID(), anchor.getGuid()));
            return;
        }
        
        // find closest to new anchor location that isn't the annotation already
        //  associated with anchor; remember that anchors are in micron
        //  coords, and we need voxels!
        TileFormat.VoxelXyz tempLocation = getTileFormat().voxelXyzForMicrometerXyz(
                new TileFormat.MicrometerXyz(anchor.getLocation().getX(),
                        anchor.getLocation().getY(),anchor.getLocation().getZ()));
        Vec3 anchorVoxelLocation = new Vec3(tempLocation.getX(),
                tempLocation.getY(), tempLocation.getZ());

        // Better to use micron location here, because the spatial index uses microns
        TmGeoAnnotation closest = annotationModel.getClosestAnnotation(anchor.getLocation(),
                annotationModel.getGeoAnnotationFromID(anchor.getNeuronID(), anchor.getGuid()));

        // check distance and other restrictions
        if (closest != null && canMergeNeurite(anchor.getNeuronID(), anchor.getGuid(), anchorVoxelLocation, closest.getNeuronId(), closest.getId())) {
            // check if user wants to merge (expensive to undo) or move (near something that
            //  is valid to merge with), or nothing (undo drag)
            Object[] options = {"Merge", "Move, don't merge", "Cancel"};
            int ans = JOptionPane.showOptionDialog(
                    ComponentUtil.getLVVMainWindow(),
                    String.format("Merge neurite from neuron %s\nto neurite in neuron %s?",
                            annotationModel.getNeuronFromNeuronID(anchor.getNeuronID()).getName(),
                            annotationModel.getNeuronFromNeuronID(closest.getNeuronId()).getName()),
                    "Merge neurites?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[2]
            );
            if (ans == JOptionPane.CANCEL_OPTION) {
                annotationModel.fireAnnotationNotMoved(annotationModel.getGeoAnnotationFromID(anchor.getNeuronID(), anchor.getGuid()));
                return;
            } else if (ans == JOptionPane.YES_OPTION) {
                activityLog.logMergedNeurite(getSampleID(), getWorkspaceID(), closest);
                mergeNeurite(anchor.getNeuronID(), anchor.getGuid(), closest.getNeuronId(), closest.getId());
            } else {
                // move, don't merge
                activityLog.logMovedNeurite(getSampleID(), getWorkspaceID(), closest);
                moveAnnotation(anchor.getNeuronID(), anchor.getGuid(), anchorVoxelLocation);
            }
        } else {
            activityLog.logMovedNeurite(getSampleID(), getWorkspaceID(), anchorVoxelLocation);
            moveAnnotation(anchor.getNeuronID(), anchor.getGuid(), anchorVoxelLocation);
        }
    }

    //-----------------------------IMPLEMENT UpdateAnchorListener
    @Override
    public void update(Anchor anchor) {
        if (anchor != null) {
            selectNeuron(anchor.getNeuronID());
        }
    }

    //-----------------------------IMPLEMENTS PathTraceListener
    @Override
    public void pathTraced(Long neuronId, AnchoredVoxelPath voxelPath) {
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
            addAnchoredPath(neuronId, endpoints, pointList);
        }
    }

    //-------------------------------IMPLEMENTS VolumeLoadListener
    @Override
    public void volumeLoaded(URL url) {
        if (initialObject instanceof TmSample) {
            activityLog.setTileFormat(getTileFormat(), initialObject.getId());
        }
        else if (initialObject instanceof TmWorkspace) {
            activityLog.setTileFormat(tileServer.getLoadAdapter().getTileFormat(), getSampleID());
        }
    }

    public TileFormat getTileFormat() {
        return tileServer.getLoadAdapter().getTileFormat();
    }

    public DomainObject getInitialObject() {
        return initialObject;
    }

    /**
     * @param initialObject = entity the user right-clicked on to start the
     * large volume viewer
     */
    public void setInitialObject(final DomainObject initialObject) {
        this.initialObject = initialObject;
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

        if (getCurrentWorkspace() == null) {
            presentError(
                    "You must load a workspace before beginning annotation!",
                    "No workspace!");
            return;
        }

        if (!editsAllowed()) {
            JOptionPane.showMessageDialog(FrameworkImplProvider.getMainFrame(), "Workspace is read-only", "Cannot add annotation", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        final TmNeuronMetadata currentNeuron = annotationModel.getCurrentNeuron();
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
                    StopWatch stopwatch = new StopWatch();
                    stopwatch.start();
                    PointRefiner refiner = new PointRefiner(quadViewUi.getSubvolumeProvider());
                    finalLocation = refiner.refine(xyz);
                    stopwatch.stop();
                    // System.out.println("refined annotation; elapsed time = " + stopwatch.toString());
                    log.info("refined annotation; elapsed time = {} ms", stopwatch.getElapsedTime());

                    // System.out.println("add annotation: input point " + xyz);
                    // System.out.println("add annotation: refined point " + finalLocation);
                } else {
                    finalLocation = xyz;
                }

                StopWatch stopwatch = new StopWatch();
                final TmWorkspace currentWorkspace = AnnotationManager.this.annotationModel.getCurrentWorkspace();
                activityLog.logAddAnchor(currentWorkspace.getSampleRef().getTargetId(), currentWorkspace.getId(), finalLocation);
                if (parentID == null) {
                    // if parentID is null, it's a new root in current neuron
                    annotationModel.addRootAnnotation(currentNeuron, finalLocation);
                } 
                else {
                    annotationModel.addChildAnnotation(currentNeuron.getGeoAnnotationMap().get(parentID), finalLocation);
                }
                stopwatch.stop();
                log.info("added annotation; elapsed time = {} ms", stopwatch.getElapsedTime());
            }

            @Override
            protected void hadSuccess() {
                // nothing here now; signals will be emitted in annotationModel
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
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
    public void deleteLink(final Long neuronID, final Long annotationID) {
        if (annotationModel.getCurrentWorkspace() == null) {
            // dialog?
            return;
        }
        
        // verify it's a link and not a root or branch:
        final TmGeoAnnotation annotation = annotationModel.getGeoAnnotationFromID(neuronID, annotationID);
        if (annotation == null) {
            presentError(
                    "No annotation to delete.",
                    "No such annotation");
            return;
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
                activityLog.logDeleteLink(getSampleID(), getWorkspaceID(), annotation);
                annotationModel.deleteLink(annotationModel.getGeoAnnotationFromID(neuronID, annotationID));
            }

            @Override
            protected void hadSuccess() {
                // nothing here; annotationModel emits signals
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };
        deleter.execute();
    }

    /**
     * delete the annotation with the input ID, and delete all of its
     * descendants
     */
    public void deleteSubTree(final Long neuronID, final Long annotationID) {
        if (annotationModel.getCurrentWorkspace() == null || annotationID == null) {
            // dialog?
            return;
        }

        // if more than one point, ask the user if they are sure (we have
        //  no undo right now!)
        final TmGeoAnnotation annotation = annotationModel.getGeoAnnotationFromID(neuronID, annotationID);
        activityLog.logDeleteSubTree(getSampleID(), getWorkspaceID(), annotation);
        int nAnnotations = annotationModel.getNeuronFromNeuronID(neuronID).getSubTreeList(annotation).size();
        if (nAnnotations > 1) {
            int ans = JOptionPane.showConfirmDialog(
                    ComponentUtil.getLVVMainWindow(),
                    String.format("Selected subtree has %d points; delete?", nAnnotations),
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
                ConsoleApp.handleException(error);
            }
        };
        deleter.execute();
    }

    /**
     * move the annotation with the input ID to the input location.
     * Activity-logged by caller.
     */
    public void moveAnnotation(final Long neuronID, final Long annotationID, final Vec3 micronLocation) {
        if (annotationModel.getCurrentWorkspace() == null) {
            // dialog?
            return;
        }
        
        SimpleWorker mover = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                annotationModel.moveAnnotation(neuronID, annotationID, micronLocation);
            }

            @Override
            protected void hadSuccess() {
                // nothing here; annotationModel will emit signals
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };
        mover.execute();
    }

    public boolean canMergeNeurite(Long neuronID, Long anchorID, Vec3 anchorLocation, Long targetNeuronID, Long targetAnnotationID) {

        // can't merge with itself
        if (anchorID.equals(targetAnnotationID)) {
            log.warn("Can't merge annotation with itself");
            return false;
        }

        TmGeoAnnotation sourceAnnotation = annotationModel.getGeoAnnotationFromID(neuronID, anchorID);
        TmGeoAnnotation targetAnnotation = annotationModel.getGeoAnnotationFromID(targetNeuronID, targetAnnotationID);

        if (sourceAnnotation==null || targetAnnotation==null) {
            return false;
        }
        
        // distance: close enough?
        double dx = anchorLocation.getX() - targetAnnotation.getX();
        double dy = anchorLocation.getY() - targetAnnotation.getY();
        double dz = anchorLocation.getZ() - targetAnnotation.getZ();
        if (dx * dx + dy * dy + dz * dz > DRAG_MERGE_THRESHOLD_SQUARED) {
            log.debug("Not close enough for merge. Distance: {},{},{}",dx,dy,dz);
            return false;
        }

        // can't merge with same neurite (don't create cycles!)
        if (annotationModel.sameNeurite(sourceAnnotation, targetAnnotation)) {
            log.debug("Can't merge with same neurite");
            return false;
        }

        return true;
    }

    /**
     * merge the two neurites to which the two annotations belong.
     * Activity-logged by caller.
     *
     * @param sourceAnnotationID
     * @param targetAnnotationID
     */
    public void mergeNeurite(final Long sourceNeuronID, final Long sourceAnnotationID, final Long targetNeuronID, final Long targetAnnotationID) {

        TmGeoAnnotation sourceAnnotation = annotationModel.getGeoAnnotationFromID(sourceNeuronID, sourceAnnotationID);
        TmGeoAnnotation targetAnnotation = annotationModel.getGeoAnnotationFromID(targetNeuronID, targetAnnotationID);
        // System.out.println("merge requested, " + sourceAnnotationID + " to " + targetAnnotationID);

        // same neurite = cycle = NO!
        // this should already be filtered out, but it's important enough to check twice
        if (annotationModel.sameNeurite(sourceAnnotation, targetAnnotation)) {
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
                annotationModel.mergeNeurite(sourceNeuronID, sourceAnnotationID, targetNeuronID, targetAnnotationID);
            }

            @Override
            protected void hadSuccess() {
                // sends its own signals
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };
        merger.execute();

    }

    public void smartMergeNeuriteRequested(Anchor sourceAnchor, Anchor targetAnchor) {
        if (targetAnchor == null) {
            presentError("No neurite selected!  Select an annotation on target neurite, then choose this operation again.",
                "No selected neurite");
            return;
        }

        TmGeoAnnotation sourceAnnotation = annotationModel.getGeoAnnotationFromID(sourceAnchor.getNeuronID(),
            sourceAnchor.getGuid());
        TmGeoAnnotation targetAnnotation = annotationModel.getGeoAnnotationFromID(targetAnchor.getNeuronID(),
            targetAnchor.getGuid());
        TmNeuronMetadata sourceNeuron = annotationModel.getNeuronFromNeuronID(sourceAnchor.getNeuronID());
        TmNeuronMetadata targetNeuron = annotationModel.getNeuronFromNeuronID(targetAnchor.getNeuronID());


        // check for cycles (this can be tested before we check exactly which annotations to merge
        if (annotationModel.sameNeurite(sourceAnnotation, targetAnnotation)) {
            presentError(
                    "You can't merge a neurite with itself!",
                    "Can't merge!");
            return;
        }


        // figure out which two annotations to merge
        List<TmGeoAnnotation> result = SmartMergeAlgorithms.mergeClosestEndpoints(sourceAnnotation,
            sourceNeuron, targetAnnotation, targetNeuron);

        if (result.size() != 2) {
            presentError("There was an error in the smart merge algorithm!", "Smart merge error");
            return;
        }
        // returned annotations should be on same neurite as input ones:
        if (!annotationModel.sameNeurite(sourceAnnotation, result.get(0)) ||
                !annotationModel.sameNeurite(targetAnnotation, result.get(1))) {
            presentError("There was an error in the smart merge algorithm!", "Smart merge error");
            return;
        }
        sourceAnnotation = result.get(0);
        targetAnnotation = result.get(1);

        // part of the smart: move the next parent someplace useful
        final Long nextParentID = SmartMergeAlgorithms.farthestEndpointNextParent(sourceAnnotation,
            sourceNeuron, targetAnnotation, targetNeuron).getId();

        // do the merge
        final Long sourceNeuronID = sourceAnnotation.getNeuronId();
        final Long sourceAnnotationID = sourceAnnotation.getId();
        final Long targetNeuronID = targetAnnotation.getNeuronId();
        final Long targetAnnotationID = targetAnnotation.getId();

        SimpleWorker merger = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                // after a smart merge, we might want to trim any dangling ends; however,
                // that probably has to be implemented in AnnModel.mergeNeurite()
                annotationModel.mergeNeurite(sourceNeuronID, sourceAnnotationID, targetNeuronID, targetAnnotationID);
            }

            @Override
            protected void hadSuccess() {
                // unlike most operations in the annmgr, we want to adjust the UI in
                //  ways other than via the updates the model triggers; in this case,
                //  we advance the next parent to the appropriate annotation
                lvvTranslator.fireNextParentEvent(nextParentID);
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };
        merger.execute();
    }

    private class TmDisplayNeuron {
    	private TmNeuronMetadata tmNeuronMetadata;
    	TmDisplayNeuron(TmNeuronMetadata tmNeuronMetadata) {
    		this.tmNeuronMetadata = tmNeuronMetadata;
    	}
    	@Override
    	public String toString() {
    		return tmNeuronMetadata.getName();
    	}
    	public TmNeuronMetadata getTmNeuronMetadata() {
    		return tmNeuronMetadata;
    	}
    }

    public void moveNeuriteRequested(Anchor anchor) {
        if (anchor == null) {
            presentError("Anchor unexpectedly null!", "Can't move neurite");
            return;
        }

        // dialog box with list of neurons, not including current neuron; but
        //  throw in a dummy "create new neuron" option at the top
        final TmGeoAnnotation annotation = annotationModel.getGeoAnnotationFromID(anchor.getNeuronID(), anchor.getGuid());
        TmNeuronMetadata sourceNeuron = annotationModel.getNeuronFromNeuronID(annotation.getNeuronId());

        ArrayList<TmNeuronMetadata> neuronList = new ArrayList<>(annotationModel.getNeuronList());
        neuronList.remove(sourceNeuron);
        // not sure alphabetical is the best sort; neuron list is selectable (defaults to creation
        //  date), but I don't want to figure out how to grab that sort order and use it here;
        //  however, alphabetical seems reasonable enough (better than arbitrary order)
        Collections.sort(neuronList, new Comparator<TmNeuronMetadata>() {
            @Override
            public int compare(TmNeuronMetadata tmNeuronMetadata, TmNeuronMetadata tmNeuronMetadata2) {
                return tmNeuronMetadata.getName().compareToIgnoreCase(tmNeuronMetadata2.getName());
            }
        });

        // add "create new" at top of sorted list
        TmNeuronMetadata dummyCreateNewNeuron = new TmNeuronMetadata();
        dummyCreateNewNeuron.setId(-1L);
        dummyCreateNewNeuron.setName("(create new neuron)");
        neuronList.add(0, dummyCreateNewNeuron);

        List<TmDisplayNeuron> displayList = new ArrayList<>();
        for(TmNeuronMetadata tmNeuronMetadata : neuronList) {
        	displayList.add(new TmDisplayNeuron(tmNeuronMetadata));
        }
        
        Object [] choices = displayList.toArray();
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
        
        final TmDisplayNeuron choiceNeuron = (TmDisplayNeuron) choice;
        final TmNeuronMetadata destinationNeuron = choiceNeuron.getTmNeuronMetadata();

        if (destinationNeuron.getId().equals(dummyCreateNewNeuron.getId())) {
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
                    TmNeuronMetadata newNeuron = annotationModel.createNeuron(neuronName);
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
                            error);
                }
            };
            mover.execute();

        } else {
            // we're moving to an existing neuron; straightforward!
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
    public void splitAnchor(final Long neuronID, Long annotationID) {
        if (annotationModel.getCurrentWorkspace() == null) {
            // dialog?
            return;
        }

        // can't split a root if it has multiple children (ambiguous):
        final TmGeoAnnotation annotation = annotationModel.getGeoAnnotationFromID(neuronID, annotationID);
        if (annotation.isRoot() && annotation.getChildIds().size() != 1) {
            presentError(
                    "Cannot split root annotation with multiple children (ambiguous)!",
                    "Error");
            return;
        }

        SimpleWorker splitter = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                activityLog.logSplitAnnotation(getSampleID(), getWorkspaceID(), annotation);
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
                        error);
            }
        };
        splitter.execute();
    }

    public void rerootNeurite(final Long neuronID, final Long newRootAnnotationID) {
        if (annotationModel.getCurrentWorkspace() == null) {
            return;
        }

        SimpleWorker rerooter = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                activityLog.logRerootNeurite(getSampleID(), getWorkspaceID(), newRootAnnotationID);
                annotationModel.rerootNeurite(neuronID, newRootAnnotationID);
            }

            @Override
            protected void hadSuccess() {
                // nothing here, model emits signals
            }

            @Override
            protected void hadError(Throwable error) {
                presentError(
                        "Could not reroot neurite!",
                        error);
            }
        };
        rerooter.execute();
    }

    public void splitNeurite(final Long neuronID, final Long newRootAnnotationID) {
        if (annotationModel.getCurrentWorkspace() == null) {
            return;
        }

        // if it's already the root, can't split
        final TmGeoAnnotation annotation = annotationModel.getGeoAnnotationFromID(neuronID, newRootAnnotationID);
        if (annotation.isRoot()) {
            presentError(
                    "Cannot split neurite at its root annotation!",
                    "Error");
            return;
        }

        SimpleWorker splitter = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                activityLog.logSplitNeurite(getSampleID(), getWorkspaceID(), annotation);
                annotationModel.splitNeurite(neuronID, annotation.getId());
            }

            @Override
            protected void hadSuccess() {
                // nothing here, model emits signals
            }

            @Override
            protected void hadError(Throwable error) {
                presentError(
                        "Could not split neurite!",
                        error);
            }
        };
        splitter.execute();

    }

    /**
     * add an anchored path; not much to check, as the UI needs to check it even
     * before the request gets here
     */
    public void addAnchoredPath(final Long neuronID, final TmAnchoredPathEndpoints endpoints, final List<List<Integer>> points) {
        if (annotationModel.getCurrentWorkspace() == null) {
            // dialog?
            return;
        }

        SimpleWorker adder = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                annotationModel.addAnchoredPath(neuronID, endpoints, points);
            }

            @Override
            protected void hadSuccess() {
                // nothing here; model sends its own signals
            }

            @Override
            protected void hadError(Throwable error) {
                presentError(
                        "Could not add anchored path!",
                        error);
            }
        };
        adder.execute();
    }

    /**
     * pop a dialog to add, edit, or delete note at the given annotation
     */
    public void addEditNote(final Long neuronID, final Long annotationID) {
        String noteText = getNote(neuronID, annotationID);

        AddEditNoteDialog testDialog = new AddEditNoteDialog(
                (Frame) SwingUtilities.windowForComponent(ComponentUtil.getLVVMainWindow()),
                noteText,
                annotationModel.getNeuronFromNeuronID(neuronID),
                annotationID);
        testDialog.setVisible(true);
        if (testDialog.isSuccess()) {
            String resultText = testDialog.getOutputText().trim();
            if (resultText.length() > 0) {
                setNote(neuronID, annotationID, resultText);
            } else {
                // empty string means delete note
                clearNote(neuronID, annotationID);
            }
        } else {
            // canceled
            return;
        }
    }

    public void clearNote(final Long neuronID, final Long annotationID) {
        TmNeuronMetadata neuron = annotationModel.getNeuronFromNeuronID(neuronID);
        final TmStructuredTextAnnotation textAnnotation = neuron.getStructuredTextAnnotationMap().get(annotationID);
        if (textAnnotation != null) {
            SimpleWorker deleter = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    annotationModel.removeNote(neuronID, textAnnotation);
                }

                @Override
                protected void hadSuccess() {
                    // nothing to see
                }

                @Override
                protected void hadError(Throwable error) {
                    presentError(
                            "Could not remove note!",
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
    public String getNote(final Long neuronID, Long annotationID) {
        return annotationModel.getNote(neuronID, annotationID);
    }

    public void setNote(final Long neuronID, final Long annotationID, final String noteText) {
        SimpleWorker setter = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                annotationModel.setNote(annotationModel.getGeoAnnotationFromID(neuronID, annotationID), noteText);
            }

            @Override
            protected void hadSuccess() {
                // nothing here
            }

            @Override
            protected void hadError(Throwable error) {
                presentError(
                        "Could not set note!",
                        error);
            }
        };
        setter.execute();
    }

    public void editNeuronTags(TmNeuronMetadata neuron) {
        log.info("editNeuronTags({})",neuron);
        // reuse the action; note that the action doesn't actually
        //  use the event, so we can throw in an empty one
        NeuronTagsAction action = new NeuronTagsAction();
        action.setTargetNeuron(neuron);
        action.actionPerformed(new ActionEvent(this, -1, "dummy event"));
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
        final String neuronName = promptForNeuronName(getNextNeuronName());

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
                            error);
                }
            };
            creator.execute();
        }
    }

    public void deleteCurrentNeuron() {
        TmNeuronMetadata neuron = annotationModel.getCurrentNeuron();
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
        final TmNeuronMetadata neuron = annotationModel.getCurrentNeuron();
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
    private String getNextNeuronName() {
        // go through existing neuron names; try to parse against
        //  standard template; create list of integers found
        ArrayList<Long> intList = new ArrayList<Long>();
        Pattern pattern = Pattern.compile("Neuron[ _]([0-9]+)");
        for (TmNeuronMetadata neuron : annotationModel.getNeuronList()) {
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
     * given an neuronId, select (make current) the neuron it belongs to
     */
    public void selectNeuron(Long neuronId) {
        if (neuronId == null) {
            return;
        }
        TmNeuronMetadata neuron = annotationModel.getNeuronFromNeuronID(neuronId);
        annotationModel.selectNeuron(neuron);
    }

    /**
     * create a new workspace with the currently loaded sample
     */
    public void createWorkspace() {
        // if no sample loaded, error
        if (initialObject == null) {
            presentError(
                    "You must load a brain sample entity before creating a workspace!",
                    "No brain sample!");
            return;
        }

        // check that the entity *is* a workspace or brain sample, and get the sample ID:
        Long sampleID;

        if (initialObject instanceof TmSample) {
            sampleID = initialObject.getId();
        }
        else if (initialObject instanceof TmWorkspace) {
            sampleID = getSampleID();
            if (sampleID == null) {
                presentError("Sample ID is null; did the previous sample or workspace finish loading?\n\nCould not create workspace!",
                        "Null sample ID");
                return;
            }
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

        EditWorkspaceNameDialog dialog = new EditWorkspaceNameDialog();
        final String workspaceName = dialog.showForSample(getAnnotationModel().getCurrentSample());
        
        if (workspaceName==null) {
            log.info("Aborting workspace creation: no valid name was provided by the user");
            return;
        }

        // create it in another thread
        // there is no doubt a better way to get these parameters in:
        final Long finalSampleId = sampleID;
        SimpleWorker creator = new SimpleWorker() {
            
            private TmWorkspace workspace;
            
            @Override
            protected void doStuff() throws Exception {
                
                log.info("Creating new workspace with name '{}' for {}",workspaceName,finalSampleId);
                
                // now we can create the workspace
                this.workspace = annotationModel.createWorkspace(finalSampleId, workspaceName);
                log.info("Created workspace with id={}",workspace.getId());

                // Reuse the existing color model 
                if (existingWorkspace) {
                    workspace.setColorModel(ModelTranslation.translateColorModel(quadViewUi.getImageColorModel()));
                    annotationModel.saveWorkspace(workspace);
                    log.info("Copied existing color model");
                }
            }

            @Override
            protected void hadSuccess() {
                LargeVolumeViewerTopComponent.getInstance().openLargeVolumeViewer(workspace);
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };
        creator.setProgressMonitor(new IndeterminateProgressMonitor(ConsoleApp.getMainFrame(), "Creating new workspace...", ""));
        creator.execute();
    }

    public void saveWorkspaceCopy() {

        EditWorkspaceNameDialog dialog = new EditWorkspaceNameDialog();
        final String workspaceName = dialog.showForSample(getAnnotationModel().getCurrentSample());
        
        if (workspaceName==null) {
            log.info("Aborting workspace creation: no valid name was provided by the user");
            return;
        }

        final TmWorkspace workspace = annotationModel.getCurrentWorkspace();
        
        SimpleWorker creator = new SimpleWorker() {
            
            private TmWorkspace workspaceCopy;
            
            @Override
            protected void doStuff() throws Exception {
                workspaceCopy = annotationModel.copyWorkspace(workspace, workspaceName);
            }

            @Override
            protected void hadSuccess() {
                LargeVolumeViewerTopComponent.getInstance().openLargeVolumeViewer(workspaceCopy);
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };

        creator.setProgressMonitor(new IndeterminateProgressMonitor(ConsoleApp.getMainFrame(), "Copying workspace...", ""));
        creator.execute();
    }

    /**
     * find an annotation relative to the input annotation in
     * a given direction; to be used in navigation along the skeleton;
     * see code for exact behavior
     */
    public Long relativeAnnotation(Long neuronId, Long annID, AnnotationNavigationDirection direction) {
        TmNeuronMetadata neuron = annotationModel.getNeuronFromNeuronID(neuronId);
        TmGeoAnnotation ann = annotationModel.getGeoAnnotationFromID(neuron, annID);
        switch (direction) {
            case ROOTWARD_JUMP:
            case ROOTWARD_STEP:
                // if root, done; anything else, move to next rootward branch,
                //  or root if there isn't one
                if (ann.isRoot()) {
                    break;
                }
                ann = neuron.getParentOf(ann);
                if (direction == AnnotationNavigationDirection.ROOTWARD_STEP) {
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
                if (direction == AnnotationNavigationDirection.ENDWARD_STEP) {
                    break;
                }
                while (!ann.isEnd() && !ann.isBranch()) {
                    ann = neuron.getChildrenOf(ann).get(0);
                }
                break;
            case NEXT_PARALLEL:
            case PREV_PARALLEL:
                // easy cases first: on root with zero or one child, nothing;
                //  on root with more than one child, take the first (or last)
                if (ann.isRoot()) {
                    if (neuron.getChildrenOf(ann).size() <= 1) {
                        break;
                    } else {
                        if (direction == AnnotationNavigationDirection.NEXT_PARALLEL) {
                            ann = neuron.getChildrenOf(ann).get(0);
                        } else {
                            // PREV_PARALLEL
                            ann = neuron.getChildrenOf(ann).get(neuron.getChildrenOf(ann).size() - 1);
                        }
                        break;
                    }
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
                int offset = (direction == AnnotationNavigationDirection.NEXT_PARALLEL) ? 1 : -1;
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
    public void chooseNeuronColor() {
        // called from annotation panel neuron gear menu "choose neuron style"
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
        // called from right-click on neuron in 2d view, "set neuron style"
        chooseNeuronStyle(annotationModel.getNeuronFromNeuronID(anchor.getNeuronID()));
    }

    public void chooseNeuronStyle(final TmNeuronMetadata neuron) {
        // called from neuron list, clicking on color swatch
        if (annotationModel.getCurrentWorkspace() == null) {
            return;
        }
        if (neuron == null) {
            // should not happen
            return;
        }

        Color color = askForNeuronColor(getNeuronStyle(neuron));
        if (color != null) {
            NeuronStyle style = new NeuronStyle(color, neuron.isVisible());
            setNeuronStyle(neuron, style);
        }
    }

    public static Color askForNeuronColor(NeuronStyle inputStyle) {
        NeuronColorDialog dialog = new NeuronColorDialog(
                (Frame) SwingUtilities.windowForComponent(ComponentUtil.getLVVMainWindow()),
                inputStyle);
        dialog.setVisible(true);
        if (dialog.styleChosen()) {
            return dialog.getChosenColor();
        } else {
            return null;
        }
    }

    public void setAllNeuronVisibility(final boolean visibility) {
        setBulkNeuronVisibility(null, visibility);
    }

    public SimpleListenableFuture setBulkNeuronVisibility(Collection<TmNeuronMetadata> neuronList, final boolean visibility) {
        final Collection<TmNeuronMetadata> neurons = neuronList==null?annotationModel.getNeuronList():neuronList;
        log.info("setBulkNeuronVisibility(neurons.size={}, visibility={})",neurons.size(),visibility);
        SimpleWorker updater = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                annotationModel.setNeuronVisibility(neurons, visibility);
            }

            @Override
            protected void hadSuccess() {
                Map<TmNeuronMetadata, NeuronStyle> updateMap = new HashMap<>();
                for (TmNeuronMetadata neuron : neurons) {
                    neuron.setVisible(visibility);
                    updateMap.put(neuron, getNeuronStyle(neuron));
                }
                annotationModel.fireNeuronStylesChanged(updateMap);
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };
        updater.setProgressMonitor(new IndeterminateProgressMonitor(ConsoleApp.getMainFrame(), visibility?"Showing neurons...":"Hiding neurons...", ""));
        return updater.executeWithFuture();
    }

    // hide others = hide all then show current; this is purely a convenience function
    public void hideUnselectedNeurons() {
        SimpleWorker updater = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                annotationModel.setNeuronVisibility(annotationModel.getNeuronList(), false);
            }

            @Override
            protected void hadSuccess() {
                Map<TmNeuronMetadata, NeuronStyle> updateMap = new HashMap<>();
                for (TmNeuronMetadata neuron : annotationModel.getNeuronList()) {
                    neuron.setVisible(false);
                    updateMap.put(neuron, getNeuronStyle(neuron));
                }
                annotationModel.fireNeuronStylesChanged(updateMap);
                setCurrentNeuronVisibility(true);
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };
        updater.setProgressMonitor(new IndeterminateProgressMonitor(ConsoleApp.getMainFrame(), "Hiding neurons...", ""));
        updater.execute();
    }
    
    public void toggleSelectedNeurons() {
        if (annotationModel.getCurrentWorkspace() == null) {
            return;
        }
        if (annotationModel.getCurrentNeuron() == null) {
            presentError("You must select a neuron to hide or show it.", "No neuron selected");
        } 
        else {
            TmNeuronMetadata currentNeuron = annotationModel.getCurrentNeuron();
            setNeuronVisibility(currentNeuron, !currentNeuron.isVisible());
        }
    }
    
    /**
     * as with chooseNeuronStyle, multiple versions allow for multiple entry points
     */
    public void setCurrentNeuronVisibility(boolean visibility) {
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
        TmNeuronMetadata neuron = annotationModel.getNeuronFromNeuronID(anchor.getNeuronID());
        setNeuronVisibility(neuron, visibility);
    }

    public void setNeuronVisibility(TmNeuronMetadata neuron, boolean visibility) {
        NeuronStyle style = getNeuronStyle(neuron);
        style.setVisible(visibility);
        setNeuronStyle(neuron, style);
    }

    public NeuronStyle getNeuronStyle(TmNeuronMetadata neuron) {
        // simple pass through; I want get/set to look the same, and set is *not*
        //  just a pass through
        return annotationModel.getNeuronStyle(neuron);
    }

    public void setNeuronStyle(final TmNeuronMetadata neuron, final NeuronStyle style) {
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
                ConsoleApp.handleException(error);
            }
        };
        setter.execute();
    }

    public void saveQuadViewColorModel() {
        log.info("saveQuadViewColorModel()");
        try {
            if (annotationModel.getCurrentWorkspace() == null) {
                presentError("You must create a workspace to be able to save the color model!", "No workspace");
            }
            else {
                TmWorkspace workspace = getCurrentWorkspace();
                workspace.setColorModel(ModelTranslation.translateColorModel(quadViewUi.getImageColorModel()));
                log.info("Setting color model: {}",workspace.getColorModel());
                saveCurrentWorkspace();
            }
        }
        catch (Exception e) {
            ConsoleApp.handleException(e);
        }
    }
    
    public void saveColorModel3d(ImageColorModel colorModel) {
        log.info("saveColorModel3d()");
        try {
            if (annotationModel.getCurrentWorkspace() == null) {
                presentError("You must create a workspace to be able to save the color model!", "No workspace");
            }
            else {
                TmWorkspace workspace = getCurrentWorkspace();
                workspace.setColorModel3d(ModelTranslation.translateColorModel(colorModel));
                log.info("Setting 3d color model: {}",workspace.getColorModel3d());
                saveCurrentWorkspace();
            }
        }
        catch (Exception e) {
            ConsoleApp.handleException(e);
        }
    }

    public void setAutomaticRefinement(final boolean state) {
        try {
            TmWorkspace workspace = getCurrentWorkspace();
            workspace.setAutoPointRefinement(state);
            saveCurrentWorkspace();
        }
        catch(Exception e) {
            ConsoleApp.handleException(e);
        }
    }

    public void setAutomaticTracing(final boolean state) {
        try {
            TmWorkspace workspace = getCurrentWorkspace();
            workspace.setAutoTracing(state);
            saveCurrentWorkspace();
        }
        catch(Exception e) {
            ConsoleApp.handleException(e);
        }
    }

    public void tracePathToParent(PathTraceToParentRequest request) {
        TmGeoAnnotation annotation = annotationModel.getGeoAnnotationFromID(request.getNeuronGuid(), request.getAnchorGuid1());
        if (annotation.isRoot()) {
            // no parent, no tracing
            return;
        }

        TmNeuronMetadata neuron = annotationModel.getNeuronFromNeuronID(annotation.getNeuronId());
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

    public void exportNeuronsAsSWC(final File swcFile, final int downsampleModulo, final Collection<TmNeuronMetadata> neurons) {
        int nannotations = 0;
        for (TmNeuronMetadata neuron : neurons) {
            nannotations += neuron.getGeoAnnotationMap().size();
        }
        if (nannotations == 0) {
            if (neurons.size()==1) {
                presentError("Neuron has no points!", "Export error");
            }
            else {
                presentError("No points in any neuron!", "Export error");
            }
            return;
        }

        BackgroundWorker saver = new BackgroundWorker() {
            
            @Override
            public String getName() {
                return "Exporting SWC File";
            }
            
            @Override
            protected void doStuff() throws Exception {
                annotationModel.exportSWCData(swcFile, downsampleModulo, neurons, this);
            }

            @Override
            public Callable<Void> getSuccessCallback() {
                return new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        DesktopApi.browse(swcFile.getParentFile());
                        return null;
                    }
                };
            }
        };
        saver.executeWithEvents();
    }

    public void importSWCFiles(final List<File> swcFiles) {
       
        if (swcFiles.isEmpty()) return;
        
        // note for the future: at this point, we could pop another dialog with:
        //  (a) info: file has xxx nodes; continue?
        //  (b) option to downsample
        //  (c) option to include/exclude automatically traced paths, if we can
        //      store that info in the file
        //  (d) option to shift position (add constant x, y, z offset)       
        
        BackgroundWorker importer = new BackgroundWorker() {

            private TmNeuronMetadata neuron;
            
            @Override
            public String getName() {
                return swcFiles.size()>1?"Importing SWC Files":"Importing SWC File";
            }

            @Override
            protected void doStuff() throws Exception {
                int imported = 0;
                int index = 0;
                int total = swcFiles.size();
                TmWorkspace workspace = annotationModel.getCurrentWorkspace();
                for (File swcFile : swcFiles) {
                    setStatus(swcFile.getName());
                    if (swcFile.exists()) {
                        neuron = annotationModel.importBulkSWCData(swcFile, workspace, null);
                        activityLog.logImportSWCFile(workspace.getId(), swcFile.getName());
                        imported++;
                    }
                    setProgress(index++, total);
                }
                setStatus("Successfully imported "+imported+" files");
            }

            @Override
            protected void hadSuccess() {
                annotationModel.postWorkspaceUpdate(neuron);
            }
        };
        importer.executeWithEvents();
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
    public void presentError(String message, Throwable error) throws HeadlessException {
        FrameworkImplProvider.handleException(new Exception(message,error));
    }

    private Long getSampleID() {
        if (annotationModel.getCurrentWorkspace() != null) {
            return annotationModel.getCurrentWorkspace().getSampleRef().getTargetId();
        } else {
            return null;
        }
    }

    private Long getWorkspaceID() {
        return annotationModel.getCurrentWorkspace().getId();
    }

    public TmWorkspace getCurrentWorkspace() {
        return annotationModel.getCurrentWorkspace();
    }
    
    public void saveCurrentWorkspace() throws Exception {
        annotationModel.saveCurrentWorkspace();
    }

    public AnnotationModel getAnnotationModel() {
        return annotationModel;
    }

    public NeuronSet getNeuronSet() {
        return annotationModel.getNeuronSet();
    }
    
    
}
