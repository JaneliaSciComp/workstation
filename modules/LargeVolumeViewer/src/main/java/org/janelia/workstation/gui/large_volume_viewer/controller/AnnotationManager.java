package org.janelia.workstation.gui.large_volume_viewer.controller;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.*;
import java.util.List;

import javax.swing.JOptionPane;

import Jama.Matrix;
import com.google.common.eventbus.Subscribe;
import org.janelia.console.viewerapi.dialogs.NeuronGroupsDialog;
import org.janelia.model.domain.tiledMicroscope.*;
import org.janelia.model.util.MatrixUtilities;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.workstation.controller.action.MergeNeuronsAction;
import org.janelia.workstation.controller.eventbus.*;
import org.janelia.workstation.controller.listener.TmAnchoredPathListener;
import org.janelia.workstation.gui.large_volume_viewer.tracing.PathTraceListener;
import org.janelia.workstation.controller.listener.TmGeoAnnotationAnchorListener;
import org.janelia.workstation.controller.listener.ViewStateListener;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.model.TmSelectionState;
import org.janelia.workstation.controller.tileimagery.VoxelPosition;
import org.janelia.workstation.geom.CoordinateAxis;
import org.janelia.workstation.gui.large_volume_viewer.LargeVolumeViewer;
import org.janelia.workstation.gui.large_volume_viewer.annotation.PointRefiner;
import org.janelia.workstation.gui.large_volume_viewer.annotation.SmartMergeAlgorithms;
import org.janelia.workstation.gui.large_volume_viewer.listener.AnchoredVoxelPathListener;
import org.janelia.workstation.gui.large_volume_viewer.listener.NextParentListener;
import org.janelia.workstation.gui.large_volume_viewer.skeleton.SkeletonController;
import org.janelia.workstation.gui.large_volume_viewer.tracing.*;
import org.janelia.workstation.gui.large_volume_viewer.QuadViewUi;
import org.janelia.workstation.controller.tileimagery.TileServer;
import org.janelia.workstation.gui.large_volume_viewer.activity_logging.ActivityLogHelper;
import org.janelia.workstation.gui.large_volume_viewer.skeleton.Skeleton;
import org.janelia.workstation.gui.large_volume_viewer.skeleton.UpdateAnchorListener;
import org.janelia.workstation.controller.tileimagery.TileFormat;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.geom.Vec3;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.gui.large_volume_viewer.ComponentUtil;
import org.janelia.workstation.gui.large_volume_viewer.skeleton.Anchor;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnnotationManager implements UpdateAnchorListener, PathTraceListener, TmAnchoredPathListener
/**
 * this class handles any actions specific to LVV tracing that can't be generalized into something common for other
 * viewers.
 */
{
    private static final Logger log = LoggerFactory.getLogger(AnnotationManager.class);
    private ActivityLogHelper activityLog = ActivityLogHelper.getInstance();
    private static final String TRACERS_GROUP = ConsoleProperties.getInstance().getProperty("console.LVVHorta.tracersgroup").trim();

    // annotation model object
    private QuadViewUi quadViewUi;
    private TileServer tileServer;
    private NeuronManager annotationModel;
    private LargeVolumeViewer largeVolumeViewer;
    private SkeletonController skeletonController;
    private Collection<AnchoredVoxelPathListener> avpListeners = new ArrayList<>();
    private Collection<TmGeoAnnotationAnchorListener> anchorListeners = new ArrayList<>();
    private Collection<NextParentListener> nextParentListeners = new ArrayList<>();
    private ViewStateListener viewStateListener;

    public void addAnchoredVoxelPathListener(AnchoredVoxelPathListener l) {
        avpListeners.add(l);
    }

    public void removeAnchoredVoxelPathListener(AnchoredVoxelPathListener l) {
        avpListeners.remove(l);
    }

    public void addTmGeoAnchorListener(TmGeoAnnotationAnchorListener l) {
        anchorListeners.add(l);
    }

    public void removeTmGeoAnchorListener(TmGeoAnnotationAnchorListener l) {
        anchorListeners.remove(l);
    }

    public void addNextParentListener(NextParentListener l) {
        nextParentListeners.add(l);
    }

    public void removeNextParentListener(NextParentListener l) {
        nextParentListeners.remove(l);
    }

    // ----- constants
    // AUTOMATIC_TRACING_TIMEOUT for automatic tracing in seconds
    private static final double AUTOMATIC_TRACING_TIMEOUT = 10.0;

    // when dragging to merge, how close in pixels (squared) to trigger
    //  a merge instead of a move
    // this distance chosen by trial and error; I annotated a neuron
    //  at what seemed like a reasonable zoom level, and I experimented
    //  until the distance threshold seemed right
    private static final double DRAG_MERGE_THRESHOLD_SQUARED = 250.0;

    public AnnotationManager(QuadViewUi quadViewUi, LargeVolumeViewer largeVolumeViewer, TileServer tileServer) {
        this.annotationModel = NeuronManager.getInstance();
        this.quadViewUi = quadViewUi;
        this.largeVolumeViewer = largeVolumeViewer;
        this.tileServer = tileServer;
        setupSignals();
    }

    private void setupSignals () {
        ViewerEventBus.registerForEvents(this);
    }

    public void connectSkeletonSignals(Skeleton skeleton, SkeletonController skeletonController) {
        this.skeletonController = skeletonController;
        addAnchoredVoxelPathListener(skeletonController);
        addNextParentListener(skeletonController);
    }

    @Subscribe
    public void workspaceLoaded(LoadProjectEvent event) {
        log.info("Workspace loaded");
        TmWorkspace workspace = event.getWorkspace();
        if (workspace != null) {

            // See about things to add to the Tile Format.
            // These must be collected from the workspace, because they
            // require knowledge of the sample ID, rather than file path.
            TileFormat tileFormat = getTileFormat();
            if (tileFormat != null) {
                TmSample sample = TmModelManager.getInstance().getCurrentSample();
                if (sample.getMicronToVoxMatrix() != null && sample.getVoxToMicronMatrix() != null) {
                    Matrix micronToVoxMatrix = MatrixUtilities.deserializeMatrix(sample.getMicronToVoxMatrix(), "micronToVoxMatrix");
                    Matrix voxToMicronMatrix = MatrixUtilities.deserializeMatrix(sample.getVoxToMicronMatrix(), "voxToMicronMatrix");
                    tileFormat.setMicronToVoxMatrix(micronToVoxMatrix);
                    tileFormat.setVoxToMicronMatrix(voxToMicronMatrix);
                }
            }

            // check for saved image color model
            if (workspace.getColorModel() != null && viewStateListener != null) {
                viewStateListener.loadColorModel(workspace.getColorModel());
            }
        }
    }

    /**
     * called when the model changes the current neuron
     */
    @Subscribe
    public void neuronSelected(SelectionNeuronsEvent event) {
        if (event.getItems() == null || event.getItems().size()==0) {
            return;
        }
        TmNeuronMetadata neuron = (TmNeuronMetadata)event.getItems().get(0);

        // if there's a selected annotation in the neuron already, don't change it:
        Anchor anchor = largeVolumeViewer.getSkeletonActor().getModel().getNextParent();
        if (anchor != null && neuron.getGeoAnnotationMap().containsKey(anchor.getGuid())) {
            return;
        }

        // if neuron has no annotations, clear old one anyway
        if (neuron.getGeoAnnotationMap().size() == 0) {
            fireNextParentEvent(null);
            return;
        }

        // find some annotation in selected neuron and select it, too
        // let's select the first endpoint we find:
        TmGeoAnnotation firstRoot = neuron.getFirstRoot();

        // not clear how it could happen, but we've seen a null here:
        if (firstRoot != null) {
            for (TmGeoAnnotation link : neuron.getSubTreeList(firstRoot)) {
                if (link.getChildIds().size() == 0) {
                    fireNextParentEvent(link.getId());
                    return;
                }
            }
        }
    }

    public void fireNextParentEvent(Long id) {
        for (NextParentListener l : nextParentListeners) {
            l.setNextParent(id);
        }
    }

    //-----------------------------IMPLEMENTS TmAnchoredPathListener
    @Override
    public void addAnchoredPath(Long neuronID, TmAnchoredPath path) {
        for (AnchoredVoxelPathListener l : avpListeners) {
            l.addAnchoredVoxelPath(TAP2AVP(neuronID, path));
        }
    }

    @Override
    public void removeAnchoredPaths(Long neuronID, List<TmAnchoredPath> pathList) {
        for (TmAnchoredPath path : pathList) {
            for (AnchoredVoxelPathListener l : avpListeners) {
                l.removeAnchoredVoxelPath(TAP2AVP(neuronID, path));
            }
        }
    }

    @Override
    public void removeAnchoredPathsByNeuronID(Long neuronID) {
        for (AnchoredVoxelPathListener l : avpListeners) {
            l.removeAnchoredVoxelPaths(neuronID);
        }
    }

    public void addAnchoredPaths(Long neuronID, List<TmAnchoredPath> pathList) {
        List<AnchoredVoxelPath> voxelPathList = new ArrayList<>();
        for (TmAnchoredPath path : pathList) {
            voxelPathList.add(TAP2AVP(neuronID, path));
        }

        for (AnchoredVoxelPathListener l : avpListeners) {
            l.addAnchoredVoxelPaths(voxelPathList);
        }
    }

    /**
     * convert between path formats
     *
     * @param path = TmAnchoredPath
     * @return corresponding AnchoredVoxelPath
     */
    private AnchoredVoxelPath TAP2AVP(final Long inputNeuronID, TmAnchoredPath path) {
        // prepare the data:
        TmAnchoredPathEndpoints endpoints = path.getEndpoints();
        final SegmentIndex inputSegmentIndex = new SegmentIndex(endpoints.getFirstAnnotationID(),
                endpoints.getSecondAnnotationID());

        final ArrayList<VoxelPosition> inputPath = new ArrayList<>();
        final CoordinateAxis axis = CoordinateAxis.Z;
        final int depthAxis = axis.index();
        final int heightAxis = axis.index() - 1 % 3;
        final int widthAxis = axis.index() - 2 % 3;
        for (List<Integer> point : path.getPointList()) {
            inputPath.add(
                    new VoxelPosition(point.get(widthAxis), point.get(heightAxis), point.get(depthAxis))
            );
        }

        // do a quick implementation of the interface:
        AnchoredVoxelPath voxelPath = new AnchoredVoxelPath() {
            Long neuronID;
            SegmentIndex segmentIndex;
            List<VoxelPosition> path;

            {
                this.neuronID = inputNeuronID;
                this.segmentIndex = inputSegmentIndex;
                this.path = inputPath;
            }

            public Long getNeuronID() {
                return neuronID;
            }

            @Override
            public SegmentIndex getSegmentIndex() {
                return segmentIndex;
            }

            @Override
            public List<VoxelPosition> getPath() {
                return path;
            }
        };

        return voxelPath;
    }

    // need common permissions model
    public boolean editsAllowed() {
        return !TmModelManager.getInstance().getCurrentView().isProjectReadOnly();
    }

    public void editNeuronGroups() {
        NeuronGroupsDialog ngDialog = new NeuronGroupsDialog();
        ngDialog.showDialog();
    }

    public void anchorAdded(Skeleton.AnchorSeed seed) {
        addAnnotation(seed.getLocation(), seed.getParentGuid());
    }

    /**
     * @param anchor
     */
    public void moveAnchor(Anchor anchor) {

        if (!editsAllowed()) {
            JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(), "Workspace is read-only", "Cannot edit annotation", JOptionPane.INFORMATION_MESSAGE);
            annotationModel.fireAnnotationNotMoved(annotationModel.getGeoAnnotationFromID(anchor.getNeuronID(), anchor.getGuid()));
            return;
        }

        if (!TmModelManager.getInstance().checkOwnership(anchor.getNeuronID()))
            return;
        
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
            // test if merge would create a loop; if so, report common parent in dialog and to
            //  clipboard, so "go to" can be used
            TmGeoAnnotation sourceAnnotation = annotationModel.getGeoAnnotationFromID(anchor.getNeuronID(), anchor.getGuid());
            TmGeoAnnotation targetAnnotation = annotationModel.getGeoAnnotationFromID(closest.getNeuronId(), closest.getId());
            TmGeoAnnotation common = annotationModel.findCommonParent(sourceAnnotation, targetAnnotation);
            if (common != null) {
                // loop found
                Vec3 loopLocation = getTileFormat().micronVec3ForVoxelVec3Centered(
                        new Vec3(common.getX(), common.getY(), common.getZ()));
                String locationString =  loopLocation.getX() + "," + loopLocation.getY() + "," + loopLocation.getZ();
                StringSelection selection = new StringSelection(locationString);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(selection, selection);
                JOptionPane.showMessageDialog(ComponentUtil.getLVVMainWindow(),
                        "Merge would create a loop; first common ancestor is at location "
                                + locationString + " (copied to clipboard).");
                annotationModel.fireAnnotationNotMoved(annotationModel.getGeoAnnotationFromID(anchor.getNeuronID(), anchor.getGuid()));
                return;
            }

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
                TmNeuronMetadata sourceNeuron = annotationModel.getNeuronFromNeuronID(anchor.getNeuronID());
                TmGeoAnnotation sourceVertex = annotationModel.getGeoAnnotationFromID(anchor.getNeuronID(), anchor.getGuid());
                TmNeuronMetadata targetNeuron = annotationModel.getNeuronFromNeuronID(closest.getNeuronId());
                TmGeoAnnotation targetVertex = annotationModel.getGeoAnnotationFromID(closest.getNeuronId(), closest.getId());
                TmSelectionState.getInstance().setCurrentNeuron(sourceNeuron);
                TmSelectionState.getInstance().setCurrentVertex(sourceVertex);
                TmSelectionState.getInstance().setSecondarySelections(targetNeuron);
                TmSelectionState.getInstance().setSecondaryVertex(targetVertex);
                MergeNeuronsAction action = new MergeNeuronsAction();
                action.execute();
            } else {
                // move, don't merge
                activityLog.logMovedAnchor(getSampleID(), getWorkspaceID(), anchor.getNeuronID(), closest);
                moveAnnotation(anchor.getNeuronID(), anchor.getGuid(), anchorVoxelLocation);
            }
        } else {
            activityLog.logMovedAnchor(getSampleID(), getWorkspaceID(), anchor.getNeuronID(), anchorVoxelLocation);
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

    public TileFormat getTileFormat() {
        return tileServer.getLoadAdapter().getTileFormat();
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

        if (TmModelManager.getInstance().getCurrentWorkspace() == null) {
            presentError(
                    "You must load a workspace before beginning annotation!",
                    "No workspace!");
            return;
        }

        if (!editsAllowed()) {
            JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(), "Workspace is read-only", "Cannot add annotation", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        final TmNeuronMetadata currentNeuron = TmSelectionState.getInstance().getCurrentNeuron();
        if (currentNeuron == null) {
            presentError(
                    "You must select a neuron before beginning annotation!",
                    "No neuron!");
            return;
        }

        if (!TmModelManager.getInstance().checkOwnership(currentNeuron))
            return;

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
                final TmWorkspace currentWorkspace = TmModelManager.getInstance().getCurrentWorkspace();
                activityLog.logAddAnchor(currentWorkspace.getSampleRef().getTargetId(), currentWorkspace.getId(),
                    currentNeuron.getId(), finalLocation);
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
                FrameworkAccess.handleException(error);
            }
        };
        adder.execute();
    }

    /**
     * move the annotation with the input ID to the input location.
     * Activity-logged by caller.
     */
    public void moveAnnotation(final Long neuronID, final Long annotationID, final Vec3 micronLocation) {
        if (TmModelManager.getInstance().getCurrentWorkspace() == null) {
            // dialog?
            return;
        }

        if (!TmModelManager.getInstance().checkOwnership(neuronID))
            return;
        
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
                FrameworkAccess.handleException(error);
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

        // can't merge if target is hidden
        TmNeuronMetadata targetNeuron = annotationModel.getNeuronFromNeuronID(targetNeuronID);
        if (!TmModelManager.getInstance().getCurrentView().isHidden(targetNeuron.getId())) {
            log.warn("Can't merge annotation to hidden neuron");
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

        return true;
    }


    @Subscribe
    public void cameraPanTo(ViewEvent event) {
        Vec3 location = new Vec3(event.getCameraFocusX(), event.getCameraFocusY(), event.getCameraFocusZ());
        TileFormat tileFormat = getTileFormat();
        quadViewUi.setCameraFocus(
                tileFormat.micronVec3ForVoxelVec3Centered(location)
        );
    }

    /**
     * called by the model when it needs an annotation's anchor moved, whether
     * because we moved it, or because the UI moved it and the operation failed,
     * and we want it moved back
     */
    // REPLACE WITH HISTORY
    /*public void unmoveAnnotation(TmGeoAnnotation annotation) {
        fireAnchorMovedBack(annotation);
    }*/


    public void smartMergeNeuriteRequested(Anchor sourceAnchor, Anchor targetAnchor) {
        if (targetAnchor == null) {
            presentError("No neurite selected!  Select an annotation on target neurite, then choose this operation again.",
                "No selected neurite");
            return;
        }

        if (!TmModelManager.getInstance().checkOwnership(targetAnchor.getNeuronID()) ||
                !TmModelManager.getInstance().checkOwnership(sourceAnchor.getNeuronID()))
            return;

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
               // lvvTranslator.fireNextParentEvent(nextParentID);
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
        };
        merger.execute();
    }

    public void setBranchReviewed(TmNeuronMetadata neuron, List<Long> annIdList) {
        List<TmGeoAnnotation> annotationList = new ArrayList<TmGeoAnnotation>();
        for (Long annId: annIdList) {
            annotationList.add(annotationModel.getGeoAnnotationFromID(neuron, annId));
        }
       // annotationModel.branchReviewed(neuron, annotationList);
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
        if (!TmModelManager.getInstance().checkOwnership(sourceNeuron))
            return;
        
        ArrayList<TmNeuronMetadata> neuronList = new ArrayList<>(annotationModel.getCurrentFilteredNeuronList());
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
            // we're moving to an existing neuron; not allowed if destination is owned by someone else;
            //  otherwise, this is straightforward
            if (!TmModelManager.getInstance().checkOwnership(destinationNeuron))
                return;



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
     * add an anchored path; not much to check, as the UI needs to check it even
     * before the request gets here
     */
    public void addAnchoredPath(final Long neuronID, final TmAnchoredPathEndpoints endpoints, final List<List<Integer>> points) {
        if (TmModelManager.getInstance().getCurrentWorkspace() == null) {
            // dialog?
            return;
        }

        if (!TmModelManager.getInstance().checkOwnership(neuronID))
            return;
        

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
            // turns out ? or * will mess with Java's file dialogs
            //  (something about how file filters works)
            if (neuronName.contains("?") || neuronName.contains("*")) {
                presentError("Neuron names can't contain the ? or * characters!", "Could not rename neuron");
                return null;
            }
            return neuronName;
        }
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

    public void setAutomaticRefinement(final boolean state) {
    try {
        TmWorkspace workspace = TmModelManager.getInstance().getCurrentWorkspace();
        workspace.setAutoPointRefinement(state);
        TmModelManager.getInstance().saveWorkspace(workspace);
    }
    catch(Exception e) {
        FrameworkAccess.handleException(e);
    }
}

    public void setAutomaticTracing(final boolean state) {
        try {
            TmWorkspace workspace = TmModelManager.getInstance().getCurrentWorkspace();
            workspace.setAutoTracing(state);
            TmModelManager.getInstance().saveWorkspace(workspace);
        }
        catch(Exception e) {
            FrameworkAccess.handleException(e);
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
        FrameworkAccess.handleException(new Exception(message,error));
    }

    private Long getSampleID() {
        if (TmModelManager.getInstance().getCurrentWorkspace() != null) {
            return TmModelManager.getInstance().getCurrentWorkspace().getSampleRef().getTargetId();
        } else {
            return null;
        }
    }

    private Long getWorkspaceID() {
        return TmModelManager.getInstance().getCurrentWorkspace().getId();
    }
}
