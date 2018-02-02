package org.janelia.it.workstation.ab2.compute;

import org.janelia.it.workstation.ab2.model.AB2NeuronSkeleton;

public interface AB2SkeletonWalkerCallback {

    public void processPosition(AB2NeuronSkeleton.Node parentNode, AB2NeuronSkeleton.Node childNode, double edgeFraction);

}
