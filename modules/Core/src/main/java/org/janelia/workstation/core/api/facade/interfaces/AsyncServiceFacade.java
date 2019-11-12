package org.janelia.workstation.core.api.facade.interfaces;

import org.janelia.model.domain.Reference;
import org.janelia.model.domain.gui.cdmip.ColorDepthSearch;
import org.janelia.workstation.core.workers.AsyncServiceMonitoringWorker;

/**
 * Async services which run on the backend.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface AsyncServiceFacade {

    AsyncServiceMonitoringWorker executeColorDepthService(ColorDepthSearch search, Reference maskRef);
}
