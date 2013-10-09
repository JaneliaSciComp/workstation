package org.janelia.it.FlyWorkstation.gui.slice_viewer.annotation;


import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.SliceViewer;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.skeleton.Anchor;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.skeleton.Skeleton;
import org.janelia.it.FlyWorkstation.signal.Signal;
import org.janelia.it.FlyWorkstation.signal.Signal1;
import org.janelia.it.FlyWorkstation.signal.Slot1;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;

import java.util.List;


/**
 * Created with IntelliJ IDEA.
 * User: olbrisd
 * Date: 7/9/13
 * Time: 2:06 PM
 *
 * this class translates between the AnnotationModel, which says things like "I changed
 * a neuron", and the SliceViewer proper, which only wants to be told what to draw.
 * this class *only* handles the viewer, not the other traditional UI elements.
 *
 * this class's slots generally connect to the AnnotationModel, while its signals go
 * out to various UI elements.
 *
 * unfortunately, this class's comments and methods tends to use "anchor" and "annotation"
 * somewhat interchangeably, which can be confusing
 */
public class SliceViewerTranslator {

    private AnnotationModel annModel;
    private SliceViewer sliceViewer;

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

    public Slot1<Vec3> cameraPanToSlot = new Slot1<Vec3>() {
        @Override
        public void execute(Vec3 location) {
            cameraPanToSignal.emit(location);
        }
    };

    // ----- signals
    public Signal1<Vec3> cameraPanToSignal = new Signal1<Vec3>();

    public Signal1<TmGeoAnnotation> anchorAddedSignal = new Signal1<TmGeoAnnotation>();
    public Signal1<TmGeoAnnotation> anchorDeletedSignal = new Signal1<TmGeoAnnotation>();
    public Signal1<TmGeoAnnotation> anchorReparentedSignal = new Signal1<TmGeoAnnotation>();
    public Signal clearSkeletonSignal = new Signal();
    public Signal clearNextParentSignal = new Signal();


    public SliceViewerTranslator(AnnotationModel annModel, SliceViewer sliceViewer) {
        this.annModel = annModel;
        this.sliceViewer = sliceViewer;

        setupSignals();
    }

    public void connectSkeletonSignals(Skeleton skeleton) {
        anchorAddedSignal.connect(skeleton.addAnchorSlot);
        anchorDeletedSignal.connect(skeleton.deleteAnchorSlot);
        anchorReparentedSignal.connect(skeleton.reparentAnchorSlot);
        clearSkeletonSignal.connect(skeleton.clearSlot);

        clearNextParentSignal.connect(sliceViewer.getSkeletonActor().clearNextParentSlot);
    }

    private void setupSignals() {
        annModel.workspaceLoadedSignal.connect(loadWorkspaceSlot);
        annModel.neuronSelectedSignal.connect(selectNeuronSlot);
        annModel.annotationAddedSignal.connect(addAnnotationSlot);
        annModel.annotationsDeletedSignal.connect(deleteAnnotationsSlot);
        annModel.anotationReparentedSignal.connect(reparentAnnotationSlot);
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

        // clear the selected parent annotation marker if it's not in the
        //  selected neuron
        Anchor anchor = sliceViewer.getSkeletonActor().getNextParent();
        if (anchor != null && !neuron.getGeoAnnotationMap().containsKey(anchor.getGuid())) {
            clearNextParentSignal.emit();
        }

        // may eventually visually style selected neuron here
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
     * called by the model when it loads a new workspace
     */
    public void workspaceLoaded(TmWorkspace workspace) {
        if (workspace == null) {
            return;
        }

        // clear existing
        clearSkeletonSignal.emit();

        // note that we must add annotations in parent-child sequence
        //  so lines get drawn correctly
        for (TmNeuron neuron: workspace.getNeuronList()) {
            for (TmGeoAnnotation root: neuron.getRootAnnotations()) {
                for (TmGeoAnnotation ann: root.getSubTreeList()) {
                    anchorAddedSignal.emit(ann);
                }
            }
        }
    }
}
