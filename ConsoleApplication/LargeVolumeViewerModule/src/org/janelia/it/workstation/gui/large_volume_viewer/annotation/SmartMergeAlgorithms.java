package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.model.domain.tiledMicroscope.TmGeoAnnotation;

public class SmartMergeAlgorithms {

    // -------------------------------------------------------
    // algorithms for finding which specific annotations to merge within
    //  two neurites, given an annotation on each neurite

    public static List<TmGeoAnnotation> mergeChosenPoints(TmGeoAnnotation selectedAnnotation,
                                                          TmGeoAnnotation targetAnnotation) {

        // for testing; merge using the two annotations that are input
        List<TmGeoAnnotation> result = new ArrayList<>();
        result.add(selectedAnnotation);
        result.add(targetAnnotation);
        return result;
    }

    public static List<TmGeoAnnotation> mergeClosestEndpoints(TmGeoAnnotation selectedAnnotation,
                                                              TmGeoAnnotation targetAnnotation) {


        List<TmGeoAnnotation> result = new ArrayList<>();


        // do stuff
        result.add(selectedAnnotation);
        result.add(targetAnnotation);


        return result;


    }


    // -------------------------------------------------------
    // algorithms for determining where to place the next parent annotation
    //  after the merge

    // default: return target annotation, which is the
    public static TmGeoAnnotation nextParentTarget(TmGeoAnnotation sourceAnnotation,
        TmGeoAnnotation targetAnnotation) {
        return targetAnnotation;
    }

    public static TmGeoAnnotation plausibleNextParent(TmGeoAnnotation sourceAnnotation,
        TmGeoAnnotation targetAnnotation) {

        // remember that we do this calculation before the merge, so the parenting
        //  may not match what you expect!

        // if the source neurite is straight, find the far end from the source annotation
        //  (which might not be at the near end, remember, but we expect it'll be close)


        // if source neurite is branched, ugh; need to move from the source annotation
        //  "away" from the target until you hit a branch




        return targetAnnotation;
    }
}
