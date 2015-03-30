package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import Jama.Matrix;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.large_volume_viewer.LargeVolumeViewer;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.*;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Anchor;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Skeleton;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyle;
import org.janelia.it.workstation.tracing.AnchoredVoxelPath;
import org.janelia.it.workstation.tracing.SegmentIndex;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.gui.large_volume_viewer.TileFormat;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.workstation.tracing.VoxelPosition;


/**
 * Created with IntelliJ IDEA.
 * User: olbrisd
 * Date: 7/9/13
 * Time: 2:06 PM
 *
 * this class translates between the AnnotationModel, which says things like "I changed
 * a neuron", and the LargeVolumeViewer proper, which only wants to be told what to draw.
 * this class *only* handles the viewer, not the other traditional UI elements.
 *
 * this class generally observes the AnnotationModel, while its events go
 * out to various UI elements.
 *
 * unfortunately, this class's comments and methods tends to use "anchor" and "annotation"
 * somewhat interchangeably, which can be confusing
 */
public class LargeVolumeViewerTranslator implements TmGeoAnnotationModListener, TmAnchoredPathListener,
    GlobalAnnotationListener, NeuronStyleChangeListener {

    private AnnotationModel annModel;
    private LargeVolumeViewer largeVolumeViewer;
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

    public LargeVolumeViewerTranslator(AnnotationModel annModel, LargeVolumeViewer largeVolumeViewer) {
        this.annModel = annModel;
        this.largeVolumeViewer = largeVolumeViewer;

        setupSignals();
    }

    public void setViewStateListener( ViewStateListener viewStateListener ) {
        this.viewStateListener = viewStateListener;
    }
    
    public void connectSkeletonSignals(Skeleton skeleton, SkeletonController skeletonController) {
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
        annModel.addGlobalAnnotationListener(this);
        annModel.addTmGeoAnnotationModListener(this);
        annModel.addTmAnchoredPathListener(this);
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

        for (TmGeoAnnotation ann: annotationList) {
            fireAnchorDeleted(ann);
        }
    }

    /**
     * called by the model when it changes the parent of an annotation
     */
    public void reparentAnnotation(TmGeoAnnotation annotation) {
        // pretty much a pass-through to the skeleton
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

    //-----------------------------IMPLEMENTS TmAnchoredPathListener
    //  This listener functions as a value-remarshalling relay to next listener.
    @Override
    public void addAnchoredPath(TmAnchoredPath path) {
        for (AnchoredVoxelPathListener l: avpListeners) {
            l.addAnchoredVoxelPath(TAP2AVP(path));
        }
    }

    @Override
    public void removeAnchoredPaths(List<TmAnchoredPath> pathList) {
        for (TmAnchoredPath path: pathList) {
            for (AnchoredVoxelPathListener l: avpListeners) {
                l.removeAnchoredVoxelPath(TAP2AVP(path));
            }
        }
    }

    public void addAnchoredPaths(List<TmAnchoredPath> pathList) {
        List<AnchoredVoxelPath> voxelPathList = new ArrayList<>();
        for (TmAnchoredPath path: pathList) {
            voxelPathList.add(TAP2AVP(path));
        }
        for (AnchoredVoxelPathListener l: avpListeners) {
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
    public void annotationReparented(TmGeoAnnotation annotation) {
        reparentAnnotation(annotation);
    }

    @Override
    public void annotationNotMoved(TmGeoAnnotation annotation) {
        unmoveAnnotation(annotation);
    }

    @Override
    public void neuronStyleChanged(TmNeuron neuron, NeuronStyle style) {
        fireNeuronStyleChangeEvent(neuron, style);
    }

    public void annotationSelected(Long id) {
        fireNextParentEvent(id);
    }
    
    /**
     * called by the model when it loads a new workspace
     */
    @Override
    public void workspaceLoaded(TmWorkspace workspace) {
        // clear existing
        fireClearAnchors();

        if (workspace != null) {
            // See about things to add to the Tile Format.
            // These must be collected from the workspace, because they
            // require knowledge of the sample ID, rather than file path.
            TileFormat tileFormat = getTileFormat();
            if (tileFormat != null) {
                Matrix micronToVoxMatrix = workspace.getMicronToVoxMatrix();
                Matrix voxToMicronMatrix = workspace.getVoxToMicronMatrix();
                if (micronToVoxMatrix != null  &&  voxToMicronMatrix != null) {                    
                    tileFormat.setMicronToVoxMatrix(micronToVoxMatrix);
                    tileFormat.setVoxToMicronMatrix(voxToMicronMatrix);
                }
                else {
                    addMatrices(workspace, tileFormat);
                }
            }
            
            // (we used to retrieve global color here; replaced by styles)

            // set styles for our neurons; if a neuron isn't in the saved map,
            //  use a default style
            Map<Long, NeuronStyle> neuronStyleMap = annModel.getNeuronStyleMap();
            NeuronStyle style;
            for (TmNeuron neuron: workspace.getNeuronList()) {
                if (neuronStyleMap.containsKey(neuron.getId())) {
                    style = neuronStyleMap.get(neuron.getId());
                } else {
                    style = NeuronStyle.getStyleForNeuron(neuron.getId());
                }
                fireNeuronStyleChangeEvent(neuron,style);
            }
            // note: we currently don't clean up old styles in the prefs that belong
            //  to deleted neurons; this is the place to do it if/when we put that in
            //  (see FW-3100); you would iterate over the style map and remove
            //  any neurons not in the workspace, then save the map back


            // check for saved image color model
            String colorModelString = workspace.getPreferences().getProperty(AnnotationsConstants.PREF_COLOR_MODEL);
            if (colorModelString != null  &&  viewStateListener != null) {
                viewStateListener.loadColorModel(colorModelString);
            }

            // note that we must add annotations in parent-child sequence
            //  so lines get drawn correctly; we must send this as one big
            //  list so the anchor update routine is run once will all anchors
            //  present rather than piecemeal (which will cause problems in
            //  some cases on workspace reloads)
            List<TmGeoAnnotation> addedAnchorList = new ArrayList<>();
            for (TmNeuron neuron: workspace.getNeuronList()) {
                for (TmGeoAnnotation root: neuron.getRootAnnotations()) {
                    addedAnchorList.addAll(neuron.getSubTreeList(root));
                }
            }
            fireAnchorsAdded(addedAnchorList);

            // draw anchored paths, too, after all the anchors are drawn
            List<TmAnchoredPath> annList = new ArrayList<>();
            for (TmNeuron neuron: workspace.getNeuronList()) {
                for (TmAnchoredPath path: neuron.getAnchoredPathMap().values()) {
                    annList.add(path);
                }
            }
            addAnchoredPaths(annList);
        }
    }
    
    /**
     * called when the model changes the current neuron
     */
    @Override
    public void neuronSelected(TmNeuron neuron) {
        if (neuron == null) {
            return;
        }

        // if there's a selected annotation in the neuron already, don't change it:
        Anchor anchor = largeVolumeViewer.getSkeletonActor().getNextParent();
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
        TmGeoAnnotation firstRoot = neuron.getRootAnnotations().get(0);
        for (TmGeoAnnotation link: neuron.getSubTreeList(firstRoot)) {
            if (link.getChildIds().size() == 0) {
                fireNextParentEvent(link.getId());
                return;
            }
        }

    }

    //------------------------------FIRING EVENTS.
    public void fireNextParentEvent(Long id) {
        for (NextParentListener l: nextParentListeners) {
            l.setNextParent(id);
        }
    }
    private void fireAnchorsAdded(List<TmGeoAnnotation> anchors) {
        for (TmGeoAnnotationAnchorListener l: anchorListeners) {
            l.anchorsAdded(anchors);
        }
    }
    private void fireAnchorAdded(TmGeoAnnotation anchor) {
        for (TmGeoAnnotationAnchorListener l: anchorListeners) {
            l.anchorAdded(anchor);
        }
        // center new anchors
        TileFormat tileFormat = getTileFormat();
        viewStateListener.setCameraFocus(
                tileFormat.micronVec3ForVoxelVec3Centered(new Vec3(anchor.getX(),
                        anchor.getY(), anchor.getZ())));
    }
    private void fireAnchorDeleted(TmGeoAnnotation anchor) {
        for (TmGeoAnnotationAnchorListener l: anchorListeners) {
            l.anchorDeleted(anchor);
        }
    }
    private void fireAnchorReparented(TmGeoAnnotation anchor) {
        for (TmGeoAnnotationAnchorListener l: anchorListeners) {
            l.anchorReparented(anchor);
        }
    }
    private void fireAnchorMovedBack(TmGeoAnnotation anchor) {
        for (TmGeoAnnotationAnchorListener l: anchorListeners) {
            l.anchorMovedBack(anchor);
        }
    }
    private void fireClearAnchors() {
        for (TmGeoAnnotationAnchorListener l: anchorListeners) {
            l.clearAnchors();
        }
    }
    private void fireNeuronStyleChangeEvent(TmNeuron neuron, NeuronStyle style) {
        for (NeuronStyleChangeListener l: neuronStyleChangeListeners) {
            l.neuronStyleChanged(neuron, style);
        }
    }
    
    /**
     * This is a lazy-add of matrices. These matrices support translation
     * between coordinate systems.
     */
    private void addMatrices(TmWorkspace workspace, TileFormat tileFormat) {
        Matrix micronToVoxMatrix;
        Matrix voxToMicronMatrix;
        // If null, the tile format can be used to construct its
        // own versions of these matrices, and saved.
        try {
            final Entity sample = ModelMgr.getModelMgr().getEntityById(workspace.getSampleID());
            micronToVoxMatrix = tileFormat.getMicronToVoxMatrix();
            voxToMicronMatrix = tileFormat.getVoxToMicronMatrix();
            final String micronToVoxString = workspace.serializeMatrix(micronToVoxMatrix, EntityConstants.ATTRIBUTE_MICRON_TO_VOXEL_MATRIX);
            final String voxToMicronString = workspace.serializeMatrix(voxToMicronMatrix, EntityConstants.ATTRIBUTE_VOXEL_TO_MICRON_MATRIX);
            SimpleWorker sw = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    ModelMgr.getModelMgr().setOrUpdateValue(sample, EntityConstants.ATTRIBUTE_MICRON_TO_VOXEL_MATRIX, micronToVoxString);
                    ModelMgr.getModelMgr().setOrUpdateValue(sample, EntityConstants.ATTRIBUTE_VOXEL_TO_MICRON_MATRIX, voxToMicronString);
                }

                @Override
                protected void hadSuccess() {
                }

                @Override
                protected void hadError(Throwable error) {
                    ModelMgr.getModelMgr().handleException(error);
                }
                
            };
            sw.execute();
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * convert between path formats
     *
     * @param path = TmAnchoredPath
     * @return corresponding AnchoredVoxelPath
     */
    private AnchoredVoxelPath TAP2AVP(TmAnchoredPath path) {
        // prepare the data:
        TmAnchoredPathEndpoints endpoints = path.getEndpoints();
        final SegmentIndex inputSegmentIndex = new SegmentIndex(endpoints.getAnnotationID1(),
                endpoints.getAnnotationID2());

        final ArrayList<VoxelPosition> inputPath = new ArrayList<>();
        final CoordinateAxis axis = CoordinateAxis.Z;
        final int depthAxis = axis.index();
        final int heightAxis = axis.index() - 1 % 3;
        final int widthAxis = axis.index() - 2 % 3;
        for (List<Integer> point: path.getPointList()) {
            inputPath.add(
                new VoxelPosition(point.get(widthAxis),point.get(heightAxis),point.get(depthAxis))
            );
        }

        // do a quick implementation of the interface:
        AnchoredVoxelPath voxelPath = new AnchoredVoxelPath() {
            SegmentIndex segmentIndex;
            List<VoxelPosition> path;

            {
                this.segmentIndex = inputSegmentIndex;
                this.path = inputPath;
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
        if (largeVolumeViewer == null ||
            largeVolumeViewer.getTileServer() == null || 
            largeVolumeViewer.getTileServer().getLoadAdapter() == null) {
            return null;
        }
        return largeVolumeViewer.getTileServer().getLoadAdapter().getTileFormat();
    }

}
