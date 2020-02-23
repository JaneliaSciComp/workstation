package org.janelia.workstation.controller.listener;

import java.util.List;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;

/**
 * Created by schauderd on 10/28/18.
 */
public interface TaskReviewListener {
    void neuronBranchReviewed(TmNeuronMetadata neuron, List<TmGeoAnnotation> vertices);
}
