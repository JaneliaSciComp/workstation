package org.janelia.workstation.gui.large_volume_viewer.controller;

import Jama.Matrix;
import org.janelia.it.jacs.shared.geom.CoordinateAxis;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.gui.large_volume_viewer.LargeVolumeViewer;
import org.janelia.workstation.controller.tileimagery.TileFormat;
import org.janelia.workstation.gui.large_volume_viewer.listener.AnchoredVoxelPathListener;
import org.janelia.workstation.controller.listener.BackgroundAnnotationListener;
import org.janelia.workstation.controller.listener.GlobalAnnotationListener;
import org.janelia.workstation.gui.large_volume_viewer.listener.NeuronStyleChangeListener;
import org.janelia.workstation.gui.large_volume_viewer.listener.NextParentListener;
import org.janelia.workstation.gui.large_volume_viewer.skeleton.SkeletonController;
import org.janelia.workstation.controller.listener.TmAnchoredPathListener;
import org.janelia.workstation.controller.listener.TmGeoAnnotationAnchorListener;
import org.janelia.workstation.controller.listener.TmGeoAnnotationModListener;
import org.janelia.workstation.controller.listener.ViewStateListener;
import org.janelia.workstation.gui.large_volume_viewer.skeleton.Anchor;
import org.janelia.workstation.gui.large_volume_viewer.skeleton.Skeleton;
import org.janelia.workstation.gui.large_volume_viewer.style.NeuronStyle;
import org.janelia.workstation.gui.large_volume_viewer.tracing.AnchoredVoxelPath;
import org.janelia.workstation.gui.large_volume_viewer.tracing.SegmentIndex;
import org.janelia.workstation.controller.tileimagery.VoxelPosition;
import org.janelia.model.domain.tiledMicroscope.TmAnchoredPath;
import org.janelia.model.domain.tiledMicroscope.TmAnchoredPathEndpoints;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.model.util.MatrixUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA. User: olbrisd Date: 7/9/13 Time: 2:06 PM
 *
 * this class translates between the NeuronManager, which says things like "I
 * changed a neuron", and the LargeVolumeViewer proper, which only wants to be
 * told what to draw. this class *only* handles the viewer, not the other
 * traditional UI elements.
 *
 * this class generally observes the NeuronManager, while its events go out to
 * various UI elements.
 *
 * unfortunately, this class's comments and methods tends to use "anchor" and
 * "annotation" somewhat interchangeably, which can be confusing
 */
public class LargeVolumeViewerTranslator implements TmGeoAnnotationModListener, TmAnchoredPathListener,
        GlobalAnnotationListener, BackgroundAnnotationListener {

    private Logger logger = LoggerFactory.getLogger(LargeVolumeViewerTranslator.class);

    private NeuronManager annModel;
    private LargeVolumeViewer largeVolumeViewer;
    private SkeletonController skeletonController;
    private Collection<AnchoredVoxelPathListener> avpListeners = new ArrayList<>();
    private Collection<TmGeoAnnotationAnchorListener> anchorListeners = new ArrayList<>();
    private Collection<NextParentListener> nextParentListeners = new ArrayList<>();
    private Collection<NeuronStyleChangeListener> neuronStyleChangeListeners = new ArrayList<>();
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

    public void addNeuronStyleChangeListener(NeuronStyleChangeListener l) {
        neuronStyleChangeListeners.add(l);
    }

    public void removeNeuronStyleChangeListener(NeuronStyleChangeListener l) {
        neuronStyleChangeListeners.remove(l);
    }

    public LargeVolumeViewerTranslator(NeuronManager annModel, LargeVolumeViewer largeVolumeViewer) {
        this.annModel = annModel;
        this.largeVolumeViewer = largeVolumeViewer;

        setupSignals();
    }

    public void setViewStateListener(ViewStateListener viewStateListener) {
        this.viewStateListener = viewStateListener;
    }

    public void connectSkeletonSignals(Skeleton skeleton, SkeletonController skeletonController) {
        this.skeletonController = skeletonController;
        addAnchoredVoxelPathListener(skeletonController);
        addTmGeoAnchorListener(skeletonController);
        addNextParentListener(skeletonController);
        addNeuronStyleChangeListener(skeletonController);
        skeletonController.registerForEvents(this);
    }

    public void cameraPanTo(Vec3 location) {
        TileFormat tileFormat = getTileFormat();
        viewStateListener.setCameraFocus(
                tileFormat.micronVec3ForVoxelVec3Centered(location)
        );
    }

    private void setupSignals() {
      //  annModel.addGlobalAnnotationListener(this);
      //  annModel.addBackgroundAnnotationListener(this);
      //  annModel.addTmGeoAnnotationModListener(this);
      //  annModel.addTmAnchoredPathListener(this);
    }

    /**
     * called when model adds a new annotation
     */
    public void addAnnotation(TmGeoAnnotation annotation) {
        if (annotation != null) {
            fireAnchorAdded(annotation);
        }
    }

    /**
     * called by the model when it deletes annotations
     */
    public void deleteAnnotations(List<TmGeoAnnotation> annotationList) {
        // remove all the individual annotations from 2D view
        int size = annotationList.size();
        int i = 0;
        for (TmGeoAnnotation ann : annotationList) {
            if (i % 100 == 0) {
                logger.info("deleteAnnotations() fireAnchorDeleted " + i + " of " + size);
            }
            fireAnchorDeleted(ann);
            i++;
        }

        // if first annotation in delete list has a parent, select it
        //  (usually if you delete a point, you want to continue working there)
        if (annotationList.size() > 0 && !annotationList.get(0).isRoot()) {
            annotationSelected(annotationList.get(0).getParentId());
        }
    }

    /**
     * called by the model when it changes the parent of an annotation
     */
    public void reparentAnnotation(TmGeoAnnotation annotation, Long prevNeuronId) {
        fireAnchorReparented(annotation);
    }

    /**
     * called by the model when it needs an annotation's anchor moved, whether
     * because we moved it, or because the UI moved it and the operation failed,
     * and we want it moved back
     */
    public void unmoveAnnotation(TmGeoAnnotation annotation) {
        fireAnchorMovedBack(annotation);
    }

    public void moveAnnotation(TmGeoAnnotation annotation) {
        fireAnchorMoved(annotation);
    }

    public void updateAnnotationRadius(TmGeoAnnotation annotation) {
        fireAnchorRadiusChanged(annotation);
    }

    public void updateNeuronRadius(TmNeuronMetadata neuron) {
        List<TmGeoAnnotation> annList = new ArrayList<>();
        for (TmGeoAnnotation root : neuron.getRootAnnotations()) {
            annList.addAll(neuron.getSubTreeList(root));
        }
        fireAnchorRadiiChanged(annList);
    }

    //-----------------------------IMPLEMENTS TmAnchoredPathListener
    //  This listener functions as a value-remarshalling relay to next listener.
    private void fireAnchoredVoxelPathsAdded(List<AnchoredVoxelPath> voxelPathList) {
        for (AnchoredVoxelPathListener l : avpListeners) {
            l.addAnchoredVoxelPaths(voxelPathList);
        }
    }

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

    //--------------------------IMPLEMENTS TmGeoAnnotationModListener
    @Override
    public void annotationAdded(TmGeoAnnotation annotation) {
        addAnnotation(annotation);
    }

    @Override
    public void annotationsDeleted(List<TmGeoAnnotation> annotations) {
        deleteAnnotations(annotations);
    }

    @Override
    public void annotationReparented(TmGeoAnnotation annotation, Long prevNeuronId) {
        reparentAnnotation(annotation, prevNeuronId);
    }

    @Override
    public void annotationNotMoved(TmGeoAnnotation annotation) {
        unmoveAnnotation(annotation);
    }

    @Override
    public void annotationMoved(TmGeoAnnotation annotation) {
        // Update LVV here, in case move came from Horta
        moveAnnotation(annotation);
    }

    @Override
    public void annotationRadiusUpdated(TmGeoAnnotation annotation) {
        updateAnnotationRadius(annotation);
    }

    @Override
    public void neuronRadiusUpdated(TmNeuronMetadata neuron) {
        updateNeuronRadius(neuron);
    }

   /* @Override
    public void neuronStyleChanged(TmNeuronMetadata neuron, NeuronStyle style) {
        fireNeuronStyleChangeEvent(neuron, style);
    }

    @Override
    public void neuronStylesChanged(Map<TmNeuronMetadata, NeuronStyle> neuronStyleMap) {
        fireNeuronStylesChangedEvent(neuronStyleMap);
    }*/

    @Override
    public void neuronTagsChanged(List<TmNeuronMetadata> neuronList) {
        // LVV translator currently does nothing with neuron tags
    }

    public void annotationSelected(Long id) {
        fireNextParentEvent(id);
    }

    @Override
    public void workspaceUnloaded(TmWorkspace workspace) {
        logger.info("Workspace unloaded");
        fireClearAnchors();
    }

    @Override
    public void workspaceLoaded(TmWorkspace workspace) {

        logger.info("Workspace loaded");

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
                } else {
                    addMatrices(tileFormat);
                }
            }

            // check for saved image color model
            if (workspace.getColorModel() != null && viewStateListener != null) {
                viewStateListener.loadColorModel(workspace.getColorModel());
            }
        }
    }

    @Override
    public void spatialIndexReady(TmWorkspace workspace) {

        logger.info("Spatial index is ready. Rebuilding anchor model.");

        if (workspace != null) {

            Map<TmNeuronMetadata, NeuronStyle> updateNeuronStyleMap = new HashMap<>();
            List<TmGeoAnnotation> addedAnchorList = new ArrayList<>();
            List<AnchoredVoxelPath> voxelPathList = new ArrayList<>();

            for (TmNeuronMetadata neuron : annModel.getNeuronList()) {

                // (we used to retrieve global color here; replaced by styles)
                // set styles for our neurons; if a neuron isn't in the saved map,
                //  use a default style
               /// NeuronStyle style = annModel.getNeuronStyle(neuron);
               // updateNeuronStyleMap.put(neuron, style);

                // note that we must add annotations in parent-child sequence
                //  so lines get drawn correctly; we must send this as one big
                //  list so the anchor update routine is run once will all anchors
                //  present rather than piecemeal (which will cause problems in
                //  some cases on workspace reloads)
                for (TmGeoAnnotation root : neuron.getRootAnnotations()) {
                    addedAnchorList.addAll(neuron.getSubTreeList(root));
                }

                // draw anchored paths, too, after all the anchors are drawn
                for (TmAnchoredPath path : neuron.getAnchoredPathMap().values()) {
                    voxelPathList.add(TAP2AVP(neuron.getId(), path));
                }
            }

            skeletonController.setSkipSkeletonChange(true);

            fireNeuronStylesChangedEvent(updateNeuronStyleMap);
            logger.debug("updated {} neuron styles", updateNeuronStyleMap.size());

            fireAnchorsAdded(addedAnchorList);
            logger.info("added {} anchors", addedAnchorList.size());

            fireAnchoredVoxelPathsAdded(voxelPathList);
            logger.debug("added {} anchored paths", voxelPathList.size());

            skeletonController.setSkipSkeletonChange(false);
            skeletonController.skeletonChanged(true);
        }
    }
    
    @Override
    public void bulkNeuronsChanged(List<TmNeuronMetadata> addList, List<TmNeuronMetadata> deleteList) {
        Map<TmNeuronMetadata, NeuronStyle> updateNeuronStyleMap = new HashMap<>();
        
        for (TmNeuronMetadata neuron: addList) {
            this.neuronModelCreated(neuron);
        }
        
        for (TmNeuronMetadata neuron: deleteList) {
            this.neuronModelDeleted(neuron);
        }
        largeVolumeViewer.getCamera().fireViewChanged();
    }

    @Override
    public void neuronCreated(TmNeuronMetadata neuron) {
        logger.info("neuronCreated: {}", neuron);

        Map<TmNeuronMetadata, NeuronStyle> updateNeuronStyleMap = new HashMap<>();
        List<TmGeoAnnotation> addedAnchorList = new ArrayList<>();
        List<TmAnchoredPath> annList = new ArrayList<>();

      //  NeuronStyle style = annModel.getNeuronStyle(neuron);
      //  updateNeuronStyleMap.put(neuron, style);

        for (TmGeoAnnotation root : neuron.getRootAnnotations()) {
            addedAnchorList.addAll(neuron.getSubTreeList(root));
        }

        for (TmAnchoredPath path : neuron.getAnchoredPathMap().values()) {
            annList.add(path);
        }

        fireAnchorsAdded(addedAnchorList);
        logger.info("  added {} anchors", addedAnchorList.size());

        fireNeuronStylesChangedEvent(updateNeuronStyleMap);
        logger.info("  updated {} neuron styles", updateNeuronStyleMap.size());

        addAnchoredPaths(neuron.getId(), annList);
        logger.info("  added {} anchored paths", annList.size());
    }

    @Override
    public void neuronDeleted(TmNeuronMetadata neuron) {
        logger.info("neuronDeleted: {}", neuron);
        fireNeuronStyleRemovedEvent(neuron);
        fireClearAnchors(neuron.getGeoAnnotationMap().values());
        removeAnchoredPathsByNeuronID(neuron.getId());
    }

    @Override
    public void neuronChanged(TmNeuronMetadata neuron) {
        logger.info("neuronChanged: {}", neuron);

        // maintain next parent selection across the delete/create
        Anchor nextParent = largeVolumeViewer.getSkeletonActor().getModel().getNextParent();

        neuronDeleted(neuron);
        neuronCreated(neuron);

        if (nextParent != null && neuron.getId().equals(nextParent.getNeuronID()) && neuron.getGeoAnnotationMap().containsKey(nextParent.getGuid())) {
            fireNextParentEvent(nextParent.getGuid());
        }

    }

    @Override
    public void neuronRenamed(TmNeuronMetadata neuron) {
        // We don't care about neuron names
    }

    @Override
    public void neuronsOwnerChanged(List<TmNeuronMetadata> neuronList) {
        // We don't care about neuron owners
    }

    /**
     * called when the model changes the current neuron
     */
    @Override
    public void neuronSelected(TmNeuronMetadata neuron) {
        if (neuron == null) {
            return;
        }

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

    //------------------------------FIRING EVENTS.
    public void fireNextParentEvent(Long id) {
        for (NextParentListener l : nextParentListeners) {
            l.setNextParent(id);
        }
    }

    private void fireAnchorsAdded(List<TmGeoAnnotation> anchors) {
        for (TmGeoAnnotationAnchorListener l : anchorListeners) {
            l.anchorsAdded(anchors);
        }
    }

    private void fireAnchorAdded(TmGeoAnnotation anchor) {
        for (TmGeoAnnotationAnchorListener l : anchorListeners) {
            l.anchorAdded(anchor);
        }
        // center new anchors
        TileFormat tileFormat = getTileFormat();
        viewStateListener.setCameraFocus(
                tileFormat.micronVec3ForVoxelVec3Centered(new Vec3(anchor.getX(),
                        anchor.getY(), anchor.getZ())));
    }

    private void fireAnchorDeleted(TmGeoAnnotation anchor) {
        for (TmGeoAnnotationAnchorListener l : anchorListeners) {
            l.anchorDeleted(anchor);
        }
    }

    private void fireAnchorReparented(TmGeoAnnotation anchor) {
        for (TmGeoAnnotationAnchorListener l : anchorListeners) {
            l.anchorReparented(anchor);
        }
    }

    private void fireAnchorMovedBack(TmGeoAnnotation anchor) {
        for (TmGeoAnnotationAnchorListener l : anchorListeners) {
            l.anchorMovedBack(anchor);
        }
    }

    private void fireAnchorMoved(TmGeoAnnotation anchor) {
        for (TmGeoAnnotationAnchorListener l : anchorListeners) {
            l.anchorMoved(anchor);
        }
    }

    private void fireAnchorRadiusChanged(TmGeoAnnotation anchor) {
        for (TmGeoAnnotationAnchorListener l : anchorListeners) {
            l.anchorRadiusChanged(anchor);
        }
    }

    private void fireAnchorRadiiChanged(List<TmGeoAnnotation> anchorList) {
        for (TmGeoAnnotationAnchorListener l : anchorListeners) {
            l.anchorRadiiChanged(anchorList);
        }
    }

    private void fireClearAnchors(Collection<TmGeoAnnotation> annotations) {
        for (TmGeoAnnotationAnchorListener l : anchorListeners) {
            l.clearAnchors(annotations);
        }
    }

    private void fireClearAnchors() {
        for (TmGeoAnnotationAnchorListener l : anchorListeners) {
            l.clearAnchors();
        }
    }

    private void fireNeuronStyleChangeEvent(TmNeuronMetadata neuron, NeuronStyle style) {
        for (NeuronStyleChangeListener l : neuronStyleChangeListeners) {
            l.neuronStyleChanged(neuron, style);
        }
    }

    private void fireNeuronStylesChangedEvent(Map<TmNeuronMetadata, NeuronStyle> neuronStyleMap) {
        for (NeuronStyleChangeListener l : neuronStyleChangeListeners) {
            l.neuronStylesChanged(neuronStyleMap);
        }
    }

    private void fireNeuronStyleRemovedEvent(TmNeuronMetadata neuron) {
        for (NeuronStyleChangeListener l : neuronStyleChangeListeners) {
            l.neuronStyleRemoved(neuron);
        }
    }

    /**
     * This is a lazy-add of matrices. These matrices support translation
     * between coordinate systems.
     */
    private void addMatrices(TileFormat tileFormat) {
        // If null, the tile format can be used to construct its
        // own versions of these matrices, and saved.
        try {
            final Matrix micronToVoxMatrix = tileFormat.getMicronToVoxMatrix();
            final Matrix voxToMicronMatrix = tileFormat.getVoxToMicronMatrix();
            SimpleWorker sw = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    //annModel.setSampleMatrices(micronToVoxMatrix, voxToMicronMatrix);
                }

                @Override
                protected void hadSuccess() {
                }

                @Override
                protected void hadError(Throwable error) {
                    logger.warn("Unable to lazy-add matrices to sample.");
                }

            };
            sw.execute();

        } catch (Exception ex) {
            logger.error("Error adding matricies to TmSample", ex);
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

    private TileFormat getTileFormat() {
        if (largeVolumeViewer == null
                || largeVolumeViewer.getTileServer() == null
                || largeVolumeViewer.getTileServer().getLoadAdapter() == null) {
            return null;
        }
        return largeVolumeViewer.getTileServer().getLoadAdapter().getTileFormat();
    }

    @Override
    public void neuronModelChanged(TmNeuronMetadata neuron) {
        logger.debug("remote NeuronDelete: {}", neuron);

        Anchor nextParent = largeVolumeViewer.getSkeletonActor().getModel().getNextParent();

        neuronModelDeleted(neuron);
        neuronModelCreated(neuron);

        if (nextParent != null && neuron.getGeoAnnotationMap().containsKey(nextParent.getGuid())) {
            fireNextParentEvent(nextParent.getGuid());
        }
    }

    @Override
    public void neuronModelCreated(TmNeuronMetadata neuron) {
        logger.debug("remote NeuronCreated: {}", neuron);

        Map<TmNeuronMetadata, NeuronStyle> updateNeuronStyleMap = new HashMap<>();
        List<TmGeoAnnotation> addedAnchorList = new ArrayList<>();
        List<TmAnchoredPath> annList = new ArrayList<>();

     //   NeuronStyle style = annModel.getNeuronStyle(neuron);
     //   updateNeuronStyleMap.put(neuron, style);

        for (TmGeoAnnotation root : neuron.getRootAnnotations()) {
            addedAnchorList.addAll(neuron.getSubTreeList(root));
        }

        for (TmAnchoredPath path : neuron.getAnchoredPathMap().values()) {
            annList.add(path);
        }

        skeletonController.remoteAnchorsAdded(addedAnchorList);
        skeletonController.remoteNeuronStylesChanged(updateNeuronStyleMap);
        List<AnchoredVoxelPath> voxelPathList = new ArrayList<>();
        for (TmAnchoredPath path : annList) {
            voxelPathList.add(TAP2AVP(neuron.getId(), path));
        }
        skeletonController.remoteAddAnchoredVoxelPaths(voxelPathList);
    }

    @Override
    public void neuronModelDeleted(TmNeuronMetadata neuron) {
        logger.debug("remote NeuronDelete: {}", neuron);
        skeletonController.neuronStyleRemoved(neuron);
        skeletonController.remoteClearAnchors(neuron.getGeoAnnotationMap().values());
        skeletonController.remoteRemoveAnchoredVoxelPaths(neuron.getId());
    }

    @Override
    public void neuronOwnerChanged(TmNeuronMetadata neuron) {

    }

}
