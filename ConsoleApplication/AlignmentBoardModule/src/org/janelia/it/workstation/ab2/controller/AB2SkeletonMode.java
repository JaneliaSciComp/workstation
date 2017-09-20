package org.janelia.it.workstation.ab2.controller;

import org.janelia.it.workstation.ab2.model.AB2SkeletonDomainObject;
import org.janelia.it.workstation.ab2.renderer.AB2Basic3DRenderer;
import org.janelia.it.workstation.ab2.renderer.AB2SkeletonRenderer;

public class AB2SkeletonMode extends AB2View3DMode {

    public AB2SkeletonMode(AB2Controller controller, AB2SkeletonRenderer renderer) {
        super(controller, renderer);
        renderer.setSkeleton(((AB2SkeletonDomainObject)controller.getDomainObject()).getSkeleton());
    }

}
