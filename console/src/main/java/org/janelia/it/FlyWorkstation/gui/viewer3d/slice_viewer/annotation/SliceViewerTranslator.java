package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.annotation;


import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.Signal1;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.Slot1;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.skeleton.Skeleton;
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

    // holds the skeleton; this is a temporary hack for short-term release;
    //  eventually we'll communicate with signals & slots instead of calling methods
    private Skeleton skeleton;

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

    public Signal1<TmGeoAnnotation> anchorAddedSignal = new Signal1<TmGeoAnnotation>();
    public Signal1<TmGeoAnnotation> anchorDeletedSignal = new Signal1<TmGeoAnnotation>();


    public void setSkeleton(Skeleton skeleton) {
        this.skeleton = skeleton;

        anchorAddedSignal.connect(skeleton.addAnchorSlot);
        anchorDeletedSignal.connect(skeleton.deleteAnchorSlot);


    }

    public SliceViewerTranslator() {

        // pass

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

        // will eventually visually style selected neuron

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
        skeleton.clear();

        // note that we must add annotations in parent-child sequence
        //  so lines get drawn correctly
        // remember, for now, we're assuming one root per neuron

        for (TmNeuron neuron: workspace.getNeuronList()) {
            TmGeoAnnotation root = neuron.getRootAnnotation();
            if (root != null) {
                for (TmGeoAnnotation ann: root.getSubTreeList()) {
                    anchorAddedSignal.emit(ann);
                }
            }
        }

    }
}
