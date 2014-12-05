package org.janelia.it.workstation.gui.large_volume_viewer.annotation;


import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.large_volume_viewer.LargeVolumeViewer;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Anchor;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Skeleton;
import org.janelia.it.workstation.octree.ZoomLevel;
import org.janelia.it.workstation.octree.ZoomedVoxelIndex;
import org.janelia.it.workstation.signal.Signal;
import org.janelia.it.workstation.signal.Signal1;
import org.janelia.it.workstation.signal.Slot1;
import org.janelia.it.workstation.tracing.AnchoredVoxelPath;
import org.janelia.it.workstation.tracing.SegmentIndex;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import org.janelia.it.workstation.gui.large_volume_viewer.TileFormat;


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
 * this class's slots generally connect to the AnnotationModel, while its signals go
 * out to various UI elements.
 *
 * unfortunately, this class's comments and methods tends to use "anchor" and "annotation"
 * somewhat interchangeably, which can be confusing
 */
public class LargeVolumeViewerTranslator {

    private AnnotationModel annModel;
    private LargeVolumeViewer largeVolumeViewer;

    // ----- slots
    public Slot1<TmWorkspace> loadWorkspaceSlot = new Slot1<TmWorkspace>() {
        @Override
        public void execute(TmWorkspace workspace) {
            workspaceLoaded(workspace);
        }
    };

    public Slot1<TmNeuron> selectNeuronSlot = new Slot1<TmNeuron>() {
        @Override
        public void execute(TmNeuron neuron) {
            neuronSelected(neuron);
        }
    };

    public Slot1<TmGeoAnnotation> addAnnotationSlot = new Slot1<TmGeoAnnotation>() {
        @Override
        public void execute(TmGeoAnnotation annotation) {
            addAnnotation(annotation);
        }
    };

    public Slot1<List<TmGeoAnnotation>> deleteAnnotationsSlot = new Slot1<List<TmGeoAnnotation>>() {
        @Override
        public void execute(List<TmGeoAnnotation> annotationList) {
            deleteAnnotations(annotationList);
        }
    };

    public Slot1<TmGeoAnnotation> reparentAnnotationSlot = new Slot1<TmGeoAnnotation>() {
        @Override
        public void execute(TmGeoAnnotation annotation) {
            reparentAnnotation(annotation);
        }
    };

    public Slot1<TmGeoAnnotation> unmoveAnnotationSlot = new Slot1<TmGeoAnnotation>() {
        @Override
        public void execute(TmGeoAnnotation annotation) {
            unmoveAnnotation(annotation);
        }
    };

    public Slot1<TmAnchoredPath> addAnchoredPathSlot = new Slot1<TmAnchoredPath>() {
        @Override
        public void execute(TmAnchoredPath path) {
            addAnchoredPath(path);
        }
    };

    public Slot1<List<TmAnchoredPath>> removeAnchoredPathsSlot = new Slot1<List<TmAnchoredPath>>() {
        @Override
        public void execute(List<TmAnchoredPath> pathList) {
            removeAnchoredPaths(pathList);
        }
    };

    public Slot1<TmGeoAnnotation> annotationClickedSlot = new Slot1<TmGeoAnnotation>() {
        @Override
        public void execute(TmGeoAnnotation annotation) {
            setNextParentSignal.emit(annotation.getId());
        }
    };

    public Slot1<Vec3> cameraPanToSlot = new Slot1<Vec3>() {
        @Override
        public void execute(Vec3 location) {
            cameraPanToSignal.emit(location);
        }
    };

    public Slot1<Color> globalAnnotationColorChangedSlot = new Slot1<Color>() {
        @Override
        public void execute(Color color) {
            // just pass through right now
            changeGlobalColorSignal.emit(color);
        }
    };

    // ----- signals
    public Signal1<Vec3> cameraPanToSignal = new Signal1<>();

    public Signal1<TmGeoAnnotation> anchorAddedSignal = new Signal1<>();
    public Signal1<List<TmGeoAnnotation>> anchorsAddedSignal = new Signal1<List<TmGeoAnnotation>>();
    public Signal1<TmGeoAnnotation> anchorDeletedSignal = new Signal1<>();
    public Signal1<TmGeoAnnotation> anchorReparentedSignal = new Signal1<>();
    public Signal1<TmGeoAnnotation> anchorMovedBackSignal = new Signal1<>();
    public Signal clearSkeletonSignal = new Signal();
    public Signal1<Long> setNextParentSignal = new Signal1<>();

    public Signal1<AnchoredVoxelPath> anchoredPathAddedSignal = new Signal1<>();
    public Signal1<List<AnchoredVoxelPath>> anchoredPathsAddedSignal = new Signal1<List<AnchoredVoxelPath>>();
    public Signal1<AnchoredVoxelPath> anchoredPathRemovedSignal = new Signal1<>();

    public Signal1<Color> changeGlobalColorSignal = new Signal1<>();
    public Signal1<String> loadColorModelSignal = new Signal1<>();

    public LargeVolumeViewerTranslator(AnnotationModel annModel, LargeVolumeViewer largeVolumeViewer) {
        this.annModel = annModel;
        this.largeVolumeViewer = largeVolumeViewer;

        setupSignals();
    }

    public void connectSkeletonSignals(Skeleton skeleton) {
        anchorAddedSignal.connect(skeleton.addAnchorSlot);
        anchorsAddedSignal.connect(skeleton.addAnchorsSlot);
        anchorDeletedSignal.connect(skeleton.deleteAnchorSlot);
        anchorReparentedSignal.connect(skeleton.reparentAnchorSlot);
        anchorMovedBackSignal.connect(skeleton.moveAnchorBackSlot);
        clearSkeletonSignal.connect(skeleton.clearSlot);

        setNextParentSignal.connect(largeVolumeViewer.getSkeletonActor().setNextParentSlot);

        anchoredPathAddedSignal.connect(skeleton.addAnchoredPathSlot);
        anchoredPathsAddedSignal.connect(skeleton.addAnchoredPathsSlot);
        anchoredPathRemovedSignal.connect(skeleton.removeAnchoredPathSlot);

        changeGlobalColorSignal.connect(largeVolumeViewer.getSkeletonActor().changeGlobalColorSlot);
    }

    private void setupSignals() {
        annModel.workspaceLoadedSignal.connect(loadWorkspaceSlot);
        annModel.neuronSelectedSignal.connect(selectNeuronSlot);

        annModel.annotationAddedSignal.connect(addAnnotationSlot);
        annModel.annotationsDeletedSignal.connect(deleteAnnotationsSlot);
        annModel.annotationReparentedSignal.connect(reparentAnnotationSlot);
        annModel.annotationNotMovedSignal.connect(unmoveAnnotationSlot);

        annModel.anchoredPathAddedSignal.connect(addAnchoredPathSlot);
        annModel.anchoredPathsRemovedSignal.connect(removeAnchoredPathsSlot);

        annModel.globalAnnotationColorChangedSignal.connect(globalAnnotationColorChangedSlot);
    }

    /**
     * called when model adds a new annotation
     */
    public void addAnnotation(TmGeoAnnotation annotation) {
        if (annotation != null) {
            anchorAddedSignal.emit(annotation);
        }
    }

    /**
     * called when the model changes the current neuron
     */
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
            setNextParentSignal.emit(null);
            return;
        }

        // find some annotation in selected neuron and select it, too
        // let's select the first endpoint we find:
        TmGeoAnnotation firstRoot = neuron.getRootAnnotations().get(0);
        for (TmGeoAnnotation link: neuron.getSubTreeList(firstRoot)) {
            if (link.getChildIds().size() == 0) {
                setNextParentSignal.emit(link.getId());
                return;
            }
        }

    }

    /**
     * called by the model when it deletes annotations
     */
    public void deleteAnnotations(List<TmGeoAnnotation> annotationList) {
        // remove all the individual annotations from 2D view

        for (TmGeoAnnotation ann: annotationList) {
            anchorDeletedSignal.emit(ann);
        }
    }

    /**
     * called by the model when it changes the parent of an annotation
     */
    public void reparentAnnotation(TmGeoAnnotation annotation) {
        // pretty much a pass-through to the skeleton
        anchorReparentedSignal.emit(annotation);
    }

    /**
     * called by the model when it needs an annotation's anchor moved, whether
     * because we moved it, or because the UI moved it and the operation failed,
     * and we want it moved back
     */
    public void unmoveAnnotation(TmGeoAnnotation annotation) {
        anchorMovedBackSignal.emit(annotation);
    }

    public void addAnchoredPath(TmAnchoredPath path) {
        anchoredPathAddedSignal.emit(TAP2AVP(path));
    }

    public void addAnchoredPaths(List<TmAnchoredPath> pathList) {
        List<AnchoredVoxelPath> voxelPathList = new ArrayList<>();
        for (TmAnchoredPath path: pathList) {
            voxelPathList.add(TAP2AVP(path));
        }
        anchoredPathsAddedSignal.emit(voxelPathList);
    }

    public void removeAnchoredPaths(List<TmAnchoredPath> pathList) {
        for (TmAnchoredPath path: pathList) {
        anchoredPathRemovedSignal.emit(TAP2AVP(path));
        }
    }

    /**
     * called by the model when it loads a new workspace
     */
    public void workspaceLoaded(TmWorkspace workspace) {
        // clear existing
        clearSkeletonSignal.emit();

        if (workspace != null) {
            // retrieve global color if present; if not, revert to default
            String globalColorString = workspace.getPreferences().getProperty(AnnotationsConstants.PREF_ANNOTATION_COLOR_GLOBAL);
            Color newColor;
            if (globalColorString != null) {
                newColor = prefColorToColor(globalColorString);
            } else {
                newColor = AnnotationsConstants.DEFAULT_ANNOTATION_COLOR_GLOBAL;
            }
            changeGlobalColorSignal.emit(newColor);

            // check for saved image color model
            String colorModelString = workspace.getPreferences().getProperty(AnnotationsConstants.PREF_COLOR_MODEL);
            if (colorModelString != null) {
                loadColorModelSignal.emit(colorModelString);
            }

            // note that we must add annotations in parent-child sequence
            //  so lines get drawn correctly
            for (TmNeuron neuron: workspace.getNeuronList()) {
                for (TmGeoAnnotation root: neuron.getRootAnnotations()) {
                    // first step in optimization; could conceivably aggregate these
                    //  lists into one big list and send signal once:
                    anchorsAddedSignal.emit(neuron.getSubTreeList(root));
                }
            }

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

        final ArrayList<ZoomedVoxelIndex> inputPath = new ArrayList<>();
        TileFormat tileFormat = largeVolumeViewer.getTileServer().getLoadAdapter().getTileFormat();
        int[] origin = tileFormat.getOrigin();
        double[] scale = tileFormat.getVoxelMicrometers();
        for (List<Integer> point: path.getPointList()) {
            inputPath.add(
                new ZoomedVoxelIndex(
                    new ZoomLevel(0),
                    (int)(point.get(0)/scale[0])-origin[0],
                    (int)(point.get(1)/scale[1])-origin[1],
                    (int)(point.get(2)/scale[2])-origin[2]   //LLF-MARKED: Consider oddball use of Z
                )
            );
        }

        // do a quick implementation of the interface:
        AnchoredVoxelPath voxelPath = new AnchoredVoxelPath() {
            SegmentIndex segmentIndex;
            List<ZoomedVoxelIndex> path;

            {
                this.segmentIndex = inputSegmentIndex;
                this.path = inputPath;
            }

            @Override
            public SegmentIndex getSegmentIndex() {
                return segmentIndex;
            }

            @Override
            public List<ZoomedVoxelIndex> getPath() {
                return path;
            }
        };

        return voxelPath;
    }

    private Color prefColorToColor(String colorString) {
        // form R:G:B:A to proper Color
        String items[] = colorString.split(":");

        return new Color(Integer.parseInt(items[0]),
            Integer.parseInt(items[1]),
            Integer.parseInt(items[2]),
            Integer.parseInt(items[3]));
    }

}
