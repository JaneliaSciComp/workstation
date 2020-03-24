package org.janelia.workstation.gui.large_volume_viewer.annotation;

import java.util.ArrayList;
import java.util.List;

import org.janelia.workstation.geom.Vec3;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;

public class SmartMergeAlgorithms {

    // -------------------------------------------------------
    // algorithms for finding which specific annotations to merge within
    //  two neurites, given an annotation on each neurite
    // input: selectedAnnotation = annotation right-clicked by user
    //        targetAnnotation = current next parent annotation on another neurite
    //        (plus neurons for each annotation)
    // output: list of two annotations that should be merged; first should be
    //        on same neurite as selected, second on same neurite as target
    // if algorithm fails to find suitable points, it should return
    //  the input ones (the mergeChosenPoints option)

    public static List<TmGeoAnnotation> mergeChosenPoints(TmGeoAnnotation selectedAnnotation,
        TmNeuronMetadata selectedNeuron, TmGeoAnnotation targetAnnotation, TmNeuronMetadata targetNeuron) {

        // for testing; merge using the two annotations that are input
        List<TmGeoAnnotation> result = new ArrayList<>();
        result.add(selectedAnnotation);
        result.add(targetAnnotation);
        return result;
    }

    public static List<TmGeoAnnotation> mergeClosestEndpoints(TmGeoAnnotation selectedAnnotation,
        TmNeuronMetadata selectedNeuron, TmGeoAnnotation targetAnnotation, TmNeuronMetadata targetNeuron) {

        // finds two endpoints, including roots, that are closest and merges there
        List<TmGeoAnnotation> selectedEnds = getNeuriteEndpoints(selectedAnnotation, selectedNeuron);
        selectedEnds.add(getNeuriteRoot(selectedAnnotation, selectedNeuron));
        List<TmGeoAnnotation> targetEnds = getNeuriteEndpoints(targetAnnotation, targetNeuron);
        targetEnds.add(getNeuriteRoot(targetAnnotation, targetNeuron));

        double minDistance = distance(selectedAnnotation, targetAnnotation);
        double dist;
        TmGeoAnnotation end1 = selectedAnnotation;
        TmGeoAnnotation end2 = targetAnnotation;
        for (TmGeoAnnotation ann1: selectedEnds) {
            for (TmGeoAnnotation ann2: targetEnds) {
                dist = distance(ann1, ann2);
                if (dist < minDistance) {
                    minDistance = dist;
                    end1 = ann1;
                    end2 = ann2;
                }
            }
        }

        List<TmGeoAnnotation> result = new ArrayList<>();
        result.add(end1);
        result.add(end2);
        return result;
    }


    // -------------------------------------------------------
    // algorithms for determining where to place the next parent annotation
    //  after the merge
    // input: the two annotations that will be merged, before the merge occurs
    //        (plus neurons for each annotation)
    // output: the annotation that should be the next parent post-merge
    // if an algorithm fails to find a suitable point, it shouldn't move
    //  the next parent (return nextParentTarget())


    public static TmGeoAnnotation nextParentTarget(TmGeoAnnotation selectedAnnotation,
        TmNeuronMetadata selectedNeuron, TmGeoAnnotation targetAnnotation, TmNeuronMetadata targetNeuron) {

        // for testing: return the current next parent annotation
        return targetAnnotation;
    }

    public static TmGeoAnnotation farthestEndpointNextParent(TmGeoAnnotation selectedAnnotation,
        TmNeuronMetadata selectedNeuron, TmGeoAnnotation targetAnnotation, TmNeuronMetadata targetNeuron) {

        // we anticipate that the "selected" neurite is short, likely straight, and being
        //  connected to the larger "target" neurite; we expect that mostly we will want
        //  the next parent moved to the far end of that straight line; so let's do
        //  that until we have data to support we want something else, and more
        //  importantly, what the something else should be

        List<TmGeoAnnotation> ends = getNeuriteEndpoints(selectedAnnotation, selectedNeuron);
        ends.add(getNeuriteRoot(selectedAnnotation, selectedNeuron));
        double farthestDistance = distance(selectedAnnotation, targetAnnotation);
        double dist;
        TmGeoAnnotation nextParent = selectedAnnotation;
        for (TmGeoAnnotation ann: ends) {
            dist = distance(ann, targetAnnotation);
            if (dist > farthestDistance) {
                farthestDistance = dist;
                nextParent = ann;
            }
        }
        return nextParent;
    }

    // -------------------------------------------------------
    // utility routines
    private static double distance(TmGeoAnnotation ann1, TmGeoAnnotation ann2) {
        // I don't believe it took me *three years* to realize that Java
        //  doesn't have an exponentiation operator...so let's do this the
        //  roundabout way; unfortunately, TmGeoAnn doesn't return Vec3;
        //  one could argue that we should add that, but for now:
        Vec3 vec1 = new Vec3(ann1.getX(), ann1.getY(), ann1.getZ());
        Vec3 vec2 = new Vec3(ann2.getX(), ann2.getY(), ann2.getZ());
        return vec1.minus(vec2).norm();
    }

    /**
     * given an annotation, find the root annotation for the neurite it's on
     */
    private static TmGeoAnnotation getNeuriteRoot(TmGeoAnnotation ann, TmNeuronMetadata neuron) {
        // adapted from AnnotationModel
        TmGeoAnnotation current = ann;
        TmGeoAnnotation parent = neuron.getParentOf(current);
        while (parent !=null) {
            current = parent;
            parent = neuron.getParentOf(current);
        }
        return current;
    }

    /**
     * return a list of endpoints in a neurite; endpoints = annotations
     * that have no children (does not include the root, which is a
     * different kind of endpoint)
     */
    private static List<TmGeoAnnotation> getNeuriteEndpoints(TmGeoAnnotation annotation, TmNeuronMetadata neuron) {
        List<TmGeoAnnotation> ends = new ArrayList<>();

        TmGeoAnnotation root = getNeuriteRoot(annotation, neuron);
        for (TmGeoAnnotation ann: neuron.getSubTreeList(root)) {
            if (ann.isEnd()) {
                ends.add(ann);
            }
        }
        return ends;
    }

}
