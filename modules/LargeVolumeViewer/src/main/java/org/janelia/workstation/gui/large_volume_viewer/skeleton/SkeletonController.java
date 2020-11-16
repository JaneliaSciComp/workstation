package org.janelia.workstation.gui.large_volume_viewer.skeleton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JComponent;

import com.google.common.eventbus.Subscribe;
import org.janelia.model.domain.tiledMicroscope.*;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.workstation.controller.eventbus.*;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.tileimagery.VoxelPosition;
import org.janelia.workstation.geom.CoordinateAxis;
import org.janelia.workstation.geom.Vec3;
import org.janelia.workstation.gui.large_volume_viewer.controller.QuadViewController;
import org.janelia.workstation.gui.large_volume_viewer.listener.*;
import org.janelia.workstation.gui.large_volume_viewer.controller.TraceMode;
import org.janelia.workstation.gui.large_volume_viewer.controller.AnnotationManager;
import org.janelia.workstation.gui.large_volume_viewer.skeleton.Skeleton.AnchorSeed;
import org.janelia.workstation.gui.large_volume_viewer.tracing.AnchoredVoxelPath;
import org.janelia.workstation.gui.large_volume_viewer.tracing.SegmentIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This hands off interesting driving info to skeleton.
 * @author fosterl
 */
public class SkeletonController implements NextParentListener {

    private static final Logger log = LoggerFactory.getLogger(SkeletonController.class);

    private Skeleton skeleton;
    private List<SkeletonActor> actors = new ArrayList<>();
    private List<JComponent> updateListeners = new ArrayList<>();
    private SkeletonAnchorListener skeletonAnchorListener;
    private AnnotationManager annoMgr;
    private QuadViewController qvController;
    private Timer meshDrawUpdateTimer;
    private boolean skipSkeletonChange=false;

    private static SkeletonController instance = new SkeletonController();
    
    private Long nextParentId = -1L;
    
    private SkeletonController() {
        registerEvents();
    }

    private void registerEvents() {
        ViewerEventBus.registerForEvents(this);
    }

    // used for bulk updates... need a better mechanism so it doesn't feel so kludgy
    public void setSkipSkeletonChange(boolean skipSkeletonChange) {
        this.skipSkeletonChange = skipSkeletonChange;
    }

    public void beginTransaction() {
        this.skipSkeletonChange = true;
    }
    
    public void endTransaction() {
        this.skipSkeletonChange = false;
        skeletonChanged();
    }

    public static SkeletonController getInstance() {
        return instance;
    }
    
    public boolean editsAllowed() {
        return annoMgr.editsAllowed();
    }
    
    /** In a "driving" client that would normally construct, call this instead. */
    public void reestablish(Skeleton skeleton, AnnotationManager annotationMgr) {
        this.skeleton = skeleton;
        this.annoMgr = annotationMgr;
        skeletonAnchorListener = new ControllerSkeletonAnchorListener();
        this.skeleton.setController(this);

        actors.clear();
        qvController = null;
    }
    
    public void registerForEvents(SkeletonActor actor) {
        this.actors.add(actor);
        actor.getModel().setNextParentByID(nextParentId);
    }
    
    public void unregister(SkeletonActor actor) {
        this.actors.remove(actor);        
    }
    
    public void registerForEvents(QuadViewController qvController) {
        this.qvController = qvController;
    }
    
    public void registerForEvents(JComponent component) {
        updateListeners.add(component);
        // Retro-kick.
        this.fireComponentUpdate();
    }
    
    public void unregister(JComponent component) {
        if (updateListeners.contains(component)) {
            updateListeners.remove(component);
        }
    }

    public Vec3 getAnnotationPosition(long annotationID ) {
        final Anchor anchor = skeleton.getAnchorByID(annotationID);
        Vec3 location = null;
        if (anchor != null) {
            location = anchor.getLocation();
        }
        return location;
    }
    
    public void setLVVFocus( Vec3 focus ) {
        qvController.setCameraFocus(focus);
    }


    // -------------------- workspace-level events
    @Subscribe
    public void workspaceNeuronsLoaded(LoadNeuronsEvent event) {
        TmWorkspace workspace = event.getWorkspace();
        loadWorkspaceNeurons(workspace);
    }

    private void loadWorkspaceNeurons(TmWorkspace workspace) {
        log.info("Workspace neurons loaded; rebuilding anchor model");
        if (workspace != null) {
            setSkipSkeletonChange(true);
            for (TmNeuronMetadata neuron : NeuronManager.getInstance().getNeuronList()) {
                processNeuronCreated(neuron);
            }
            skeletonChanged(true);
            setSkipSkeletonChange(false);
        }
    }

    @Subscribe
    public void workspaceUnloaded(UnloadProjectEvent event) {
        log.info("Workspace unloaded");
        clearAnchors();
    }

    // -------------------- neuron-level events
    @Subscribe
    public void neuronCreated(NeuronCreateEvent event) {
        for (TmNeuronMetadata neuron: event.getNeurons()) {
            log.info("neuronCreated: {}", neuron);
            processNeuronCreated(neuron);
        }
    }

    @Subscribe
    public void neuronDeleted(NeuronDeleteEvent event) {
        Collection<TmNeuronMetadata> neurons = event.getNeurons();
        TmNeuronMetadata neuron=null;
        if (!neurons.isEmpty()) {
            neuron = neurons.iterator().next();
        }
        processNeuronDeleted(neuron);
    }

    @Subscribe
    public void neuronChanged(NeuronUpdateEvent event) {
        Collection<TmNeuronMetadata> neurons = event.getNeurons();
        TmNeuronMetadata neuron=null;
        if (neurons!=null && !neurons.isEmpty()) {
            neuron = neurons.iterator().next();
        } else {
            return;
        }


        // maintain next parent selection across the delete/create
        // not sure how to get next parent yet
        // Anchor nextParent = largeVolumeViewer.getSkeletonActor().getModel().getNextParent();



        processNeuronDeleted(neuron);
        NeuronCreateEvent nce = new NeuronCreateEvent(this,
                event.getNeurons());
        neuronCreated(nce);

        // see above; don't know how to get next parent yet
        /*
        if (nextParent != null && neuron.getId().equals(nextParent.getNeuronID()) && neuron.getGeoAnnotationMap().containsKey(nextParent.getGuid())) {
            setNextParent(nextParent.getGuid());
        }
         */


    }

    private void processNeuronCreated(TmNeuronMetadata neuron) {
        List<TmGeoAnnotation> addedAnchorList = new ArrayList<>();
        List<TmAnchoredPath> annList = new ArrayList<>();

        // note that we must add annotations in parent-child sequence
        //  so lines get drawn correctly; we must send this as one big
        //  list so the anchor update routine is run once will all anchors
        //  present rather than piecemeal (which will cause problems in
        //  some cases on workspace reloads)
        for (TmGeoAnnotation root : neuron.getRootAnnotations()) {
            addedAnchorList.addAll(neuron.getSubTreeList(root));
        }

        for (TmAnchoredPath path : neuron.getAnchoredPathMap().values()) {
            annList.add(path);
        }

        processAnchorsAdded(addedAnchorList, null);
        //log.info("  added {} anchors", addedAnchorList.size());

        processAnchoredVoxelPathsAdded(TAPlist2AVPlist(neuron.getId(), annList));
        //log.info("  added {} anchored paths", annList.size());
    }

    private void processNeuronDeleted(TmNeuronMetadata neuron) {
        processAnchorsDeleted(neuron.getGeoAnnotationMap().values(), null);
        processAnchoredVoxelPathsDeleted(neuron.getId());
    }



    // -------------------- anchored path-related events
    @Subscribe
    public void anchoredPathsAdded(AnchoredPathCreateEvent event) {
        Collection<TmAnchoredPath> pathList = event.getPaths();
        processAnchoredVoxelPathsAdded(TAPlist2AVPlist(event.getNeuronID(), new ArrayList<>(pathList)));
    }


    @Subscribe
    public void anchoredPathsDeleted(AnchoredPathDeleteEvent event) {
        processAnchoredVoxelPathsDeleted(TAPlist2AVPlist(event.getNeuronID(), event.getPaths()));
    }

    private List<AnchoredVoxelPath> TAPlist2AVPlist(Long neuronID, Collection<TmAnchoredPath> pathList) {
        List<AnchoredVoxelPath> result = new ArrayList<>();
        for (TmAnchoredPath path: pathList) {
            result.add(TAP2AVP(neuronID, path));
        }
        return result;
    }

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

    public void processAnchoredVoxelPathsAdded(List<AnchoredVoxelPath> paths) {
        skeleton.addTracedSegments(paths);
        skeleton.incrementAnchorVersion();
        skeletonChanged();
    }

    public void processAnchoredVoxelPathsDeleted(Long neuronID) {
        skeleton.removeTracedSegments(neuronID);
        skeletonChanged();
    }

    public void processAnchoredVoxelPathsDeleted(List<AnchoredVoxelPath> paths) {
        for (AnchoredVoxelPath avp: paths) {
            skeleton.removeTracedSegment(avp);
        }
        skeletonChanged();
    }


    // -------------------- anchor-related events
    @Subscribe
    public void anchorsAdded(AnnotationCreateEvent event) {
        Collection<TmGeoAnnotation> annotations = event.getAnnotations();
        processAnchorsAdded(annotations, event.getRequestedNextParent());
    }

    private void processAnchorsAdded(Collection<TmGeoAnnotation> vertexList, TmGeoAnnotation nextParent) {
        List<Anchor> anchors = skeleton.addTmGeoAnchors(new ArrayList<>(vertexList));
        for (Anchor anchor: anchors) {
            anchor.setSkeletonAnchorListener(skeletonAnchorListener);
        }
        if (nextParent != null) {
            setNextParent(nextParent.getId());
        } else {
            // if there is only one anchor added, make it the next parent; otherwise,
            //  do nothing
            if (anchors.size() == 1) {
                setNextParent(anchors.get(0));
            }
        }
        skeletonChanged();
    }

    @Subscribe
    public void anchorDeleted(AnnotationDeleteEvent event) {
        Collection<TmGeoAnnotation> annotations = event.getAnnotations();
        processAnchorsDeleted(annotations, event.getRequestedNextParent());
    }

    private void processAnchorsDeleted(Collection<TmGeoAnnotation> annotations,
        TmGeoAnnotation nextParent) {
        for (TmGeoAnnotation annotation: annotations) {
            skeleton.deleteTmGeoAnchor(annotation);
        }
        if (nextParent != null) {
            setNextParent(nextParent.getId());
        }
        skeletonChanged();
    }

    @Subscribe
    public void anchorReparented(AnnotationParentReparentedEvent event) {
        Collection<TmGeoAnnotation> annotations = event.getAnnotations();
        if (!annotations.isEmpty()) {
            for (TmGeoAnnotation annotation: annotations) {
                skeleton.reparentTmGeoAnchor(annotation);
            }
        }
        if (event.hasRequestedNextParent()) {
            setNextParent(event.getRequestedNextParent().getId());
        }
        skeletonChanged();
    }

    @Subscribe
    public void anchorMoved(AnnotationUpdateEvent event) {
        Collection<TmGeoAnnotation> annotations = event.getAnnotations();
        if (!annotations.isEmpty()) {
            for (TmGeoAnnotation annotation: annotations) {
                skeleton.moveTmGeoAnchor(annotation);
            }
        }
        if (event.hasRequestedNextParent()) {
            setNextParent(event.getRequestedNextParent().getId());
            skeletonChanged();
        }
    }

    public void clearAnchors() {
        skeleton.clear();
		skeletonChanged();
    }

    //--------------------------------IMPLEMENTS NextParentListener
    @Override
    public void setNextParent(Long id) {
        nextParentId = id;
        for (SkeletonActor actor: actors) {
            actor.getModel().setNextParentByID(id);
        }
        fireComponentUpdate();
    }
    
    @Override
    public void setNextParent(Anchor parent) {
        for (SkeletonActor actor : actors) {
            actor.getModel().setNextParent(parent);
        }
        fireComponentUpdate();
    }

    @Subscribe
    public void annotationSelected(SelectionAnnotationEvent event) {
        List items = event.getItems();
        if (!items.isEmpty()) {
            TmGeoAnnotation selectedAnnotation = (TmGeoAnnotation)items.get(0);
            setNextParent(selectedAnnotation.getId());
        }
    }

    public void skeletonChanged() {
        skeletonChanged(false);
    }
    
    public void skeletonChanged(boolean forceUpdate) {
        if (!skipSkeletonChange) {
            for (SkeletonActor actor : actors) {
                if (forceUpdate) {
                    actor.getModel().forceUpdateAnchors();    
                }
                else {
                    actor.getModel().updateAnchors();
                }
            }
            refreshMeshDrawUpdateTimer();
            fireComponentUpdate();
            log.debug("anchor time=" + TraceMode.getTimerMs());
        }
    }

    public void moveNeuriteRequested(Anchor anchor) {
        annoMgr.moveNeuriteRequested(anchor);
    }
    
    public boolean checkOwnership(Long neuronID) {
        return TmModelManager.getInstance().checkOwnership(neuronID);
    }

    public void smartMergeNeuriteRequested(Anchor clickedAnchor, Anchor currentParentAnchor) {
        annoMgr.smartMergeNeuriteRequested(clickedAnchor, currentParentAnchor);
    }
    
    public void anchorAdded(AnchorSeed anchorSeed) {
        annoMgr.anchorAdded(anchorSeed);
    }
    
    public void pathTraceRequested(Long neuronId, Long annotationId) {
        qvController.pathTraceRequested(neuronId, annotationId);
    }

    public void navigationRelative(Long neuronId, Long annotationId, AnnotationNavigationDirection direction) {
        Anchor anchor = skeleton.getAnchorByID(annoMgr.relativeAnnotation(neuronId, annotationId, direction));
        setNextParent(anchor);
        qvController.setCameraFocus(anchor.getLocation());
    }

    public void fireComponentUpdate() {
        for (JComponent updateListener : updateListeners) {
            updateListener.validate();
            updateListener.repaint();
        }
    }

    /**
     * Some events, being very numerous, cause a deluge of requests, each
     * of which is time-consuming to honor.  Rather than honor each one,
     * a timer/delay mechanism is in place to allow a single refresh to
     * be carried out, after things become quiescent.
     */
    private void refreshMeshDrawUpdateTimer() {
        if (meshDrawUpdateTimer != null) {
            meshDrawUpdateTimer.cancel();
        }
        meshDrawUpdateTimer = new Timer();
        TimerTask meshDrawUpdateTask = new TimerTask() {
            @Override
            public void run() {
                fireComponentUpdate();
            }
        };
        meshDrawUpdateTimer.schedule(meshDrawUpdateTask, 10000);
    }

    private class ControllerSkeletonAnchorListener implements SkeletonAnchorListener {

        @Override
        public void anchorMoved(Anchor anchor) {
            annoMgr.moveAnchor(anchor);
            skeleton.incrementAnchorVersion();
            skeletonChanged();
        }
        
        @Override
        public void anchorMovedSilent(Anchor anchor) {
            skeletonChanged(true); // update display, but don't update all listeners
        }
        
    }

}
