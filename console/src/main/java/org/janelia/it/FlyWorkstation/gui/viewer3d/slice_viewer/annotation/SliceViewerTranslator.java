package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.annotation;


import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.Signal1;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.Slot1;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.skeleton.Skeleton;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;



/**
 * Created with IntelliJ IDEA.
 * User: olbrisd
 * Date: 7/9/13
 * Time: 2:06 PM
 *
 * This class translates between the AnnotationModel, which says things like "I changed
 * a neuron", to the SliceViewer proper, which only wants to be told what to draw
 *
 */
public class SliceViewerTranslator {

    // holds the skeleton; this is a temporary hack for short-term release;
    //  eventually we'll communicate with signals & slots instead of calling methods
    private Skeleton skeleton;

    public Slot1<TmNeuron> loadNeuronSlot = new Slot1<TmNeuron>() {
        @Override
        public void execute(TmNeuron neuron) {
            neuronLoaded(neuron);
        }
    };

    public Signal1<TmGeoAnnotation> anchorAddedSignal = new Signal1<TmGeoAnnotation>();

    public void setSkeleton(Skeleton skeleton) {
        this.skeleton = skeleton;
    }


    public SliceViewerTranslator() {

        // pass

    }

    public void neuronLoaded(TmNeuron neuron) {
        if (neuron == null) {
            return;
        }

        // clear existing
        skeleton.clear();

        // load new; this is a cheat; we're going to send the same signal
        //  that annModel would
        System.out.println("original ordering");
        for (TmGeoAnnotation ann: neuron.getGeoAnnotationMap().values()) {
            System.out.println(String.format("drawing ann at %f, %f", ann.getX(), ann.getY()));
            // anchorAddedSignal.emit(ann);
        }

        // replace above with a version that adds annotations in parent-child sequence,
        //  so lines get drawn correctly; remember, for now, we're assuming one root
        System.out.println("alternate ordering");
        TmGeoAnnotation root = neuron.getRootAnnotation();
        if (root != null) {
            for (TmGeoAnnotation ann: root.getSubTreeList()) {
                 System.out.println(String.format("drawing ann at %f, %f", ann.getX(), ann.getY()));
                 anchorAddedSignal.emit(ann);
            }
        }
    }

}
