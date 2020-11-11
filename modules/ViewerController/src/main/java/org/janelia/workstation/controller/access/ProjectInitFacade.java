package org.janelia.workstation.controller.access;

import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;

public interface ProjectInitFacade {
    public void loadImagery(TmSample sample);
    public void loadAnnotationData(TmWorkspace currProject);
    public void clearViewers();
    public void notifyViewers();
}
