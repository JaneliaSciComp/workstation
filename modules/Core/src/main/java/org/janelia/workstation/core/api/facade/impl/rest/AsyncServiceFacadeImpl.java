package org.janelia.workstation.core.api.facade.impl.rest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.janelia.model.domain.gui.cdmip.ColorDepthSearch;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.facade.interfaces.AsyncServiceFacade;
import org.janelia.workstation.core.api.web.AsyncServiceClient;
import org.janelia.workstation.core.workers.AsyncServiceMonitoringWorker;
import org.janelia.workstation.core.workers.SearchMonitoringWorker;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AsyncServiceFacadeImpl implements AsyncServiceFacade {

    private AsyncServiceClient asyncServiceClient = new AsyncServiceClient();

    public AsyncServiceMonitoringWorker executeColorDepthService(ColorDepthSearch search) {
        ActivityLogHelper.logUserAction("AsyncServiceFacadeImpl.executeColorDepthService", search);
        Long serviceId = asyncServiceClient.invokeService("colorDepthObjectSearch",
                ImmutableList.of("-searchId", search.getId().toString()),
                null,
                ImmutableMap.of());
        AsyncServiceMonitoringWorker executeWorker = new SearchMonitoringWorker(search, serviceId);
        executeWorker.executeWithEvents();
        return executeWorker;
    }

}
