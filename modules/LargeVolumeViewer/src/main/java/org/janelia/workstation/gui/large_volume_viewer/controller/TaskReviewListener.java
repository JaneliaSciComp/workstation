package org.janelia.workstation.gui.large_volume_viewer.controller;

import java.util.List;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;

/**
 * Created by schauderd on 10/28/18.
 */
public interface TaskReviewListener {
    void neuronBranchReviewed(TmNeuronMetadata neuron, List<TmGeoAnnotation> vertices);
}
