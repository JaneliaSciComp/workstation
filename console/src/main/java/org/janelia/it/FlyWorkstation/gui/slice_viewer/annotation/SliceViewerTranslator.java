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
 * This class translates between the AnnotationModel, which says things like "I changed
 * a neuron", and the SliceViewer proper, which only wants to be told what to draw
 *
 */
public class SliceViewerTranslator {

    private AnnotationModel annModel;
    private SliceViewer sliceViewer;

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

    public Slot1<TmGeoAnnotation> addAnchorSlot = new Slot1<TmGeoAnnotation>() {
        @Override
        public void execute(TmGeoAnnotation annotation) {
            addAnchor(annotation);
        }
    };

    public Slot1<List<TmGeoAnnotation>> deleteAnchorsSlot = new Slot1<List<TmGeoAnnotation>>() {
        @Override
        public void execute(List<TmGeoAnnotation> annotationList) {
            anchorsDeleted(annotationList);
        }
    };

    public Signal1<Vec3> cameraPanToSignal = new Signal1<Vec3>();

    public Slot1<Vec3> cameraPanToSlot = new Slot1<Vec3>() {
        @Override
        public void execute(Vec3 location) {
            cameraPanToSignal.emit(location);
        }
    };

    public Signal1<TmGeoAnnotation> anchorAddedSignal = new Signal1<TmGeoAnnotation>();
    public Signal1<TmGeoAnnotation> anchorDeletedSignal = new Signal1<TmGeoAnnotation>();
    public Signal clearSkeletonSignal = new Signal();
    public  Signal clearNextParentSignal = new Signal();

    public SliceViewerTranslator(AnnotationModel annModel, SliceViewer sliceViewer) {

        this.annModel = annModel;
        this.sliceViewer = sliceViewer;

        setupSignals();

    }

    public void connectSkeletonSignals(Skeleton skeleton) {
        anchorAddedSignal.connect(skeleton.addAnchorSlot);
        anchorDeletedSignal.connect(skeleton.deleteAnchorSlot);
        clearSkeletonSignal.connect(skeleton.clearSlot);

        clearNextParentSignal.connect(sliceViewer.getSkeletonActor().clearNextParentSlot);

    }

    private void setupSignals() {
        // things the model tells us to do:
        annModel.workspaceLoadedSignal.connect(loadWorkspaceSlot);
        annModel.neuronSelectedSignal.connect(selectNeuronSlot);
        annModel.anchorAddedSignal.connect(addAnchorSlot);
        annModel.anchorsDeletedSignal.connect(deleteAnchorsSlot);

        // things we want done:
        

    }

    public void addAnchor(TmGeoAnnotation annotation) {
        if (annotation != null) {
            anchorAddedSignal.emit(annotation);
        }
    }

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

    public void anchorsDeleted(List<TmGeoAnnotation> annotationList) {
        // remove all the individual annotations from 2D view

        for (TmGeoAnnotation ann: annotationList) {
            anchorDeletedSignal.emit(ann);
        }

    }

    public void workspaceLoaded(TmWorkspace workspace) {
        if (workspace == null) {
            return;
        }

        // clear existing
        clearSkeletonSignal.emit();

        // note that we must add annotations in parent-child sequence
        //  so lines get drawn correctly
        // remember, for now, we're assuming one root per neuron

        for (TmNeuron neuron: workspace.getNeuronList()) {
            for (TmGeoAnnotation root: neuron.getRootAnnotations()) {
                for (TmGeoAnnotation ann: root.getSubTreeList()) {
                    anchorAddedSignal.emit(ann);
                }
            }
        }

    }
}
