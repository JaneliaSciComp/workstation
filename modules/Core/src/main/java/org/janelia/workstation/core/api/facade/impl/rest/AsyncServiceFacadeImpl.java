package org.janelia.workstation.core.api.facade.impl.rest;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import org.janelia.model.domain.Reference;
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

    public AsyncServiceMonitoringWorker executeColorDepthService(ColorDepthSearch search, Reference maskRef) {

        // Assemble parameters
        List<String> args = new ArrayList<>();
        args.add("-searchId");
        args.add(search.getId().toString());
        if (maskRef!=null) {
            args.add("-maskId");
            args.add(maskRef.getTargetId().toString());
        }
        args.add("-use-java-process");

        // Invoke the service
        ActivityLogHelper.logUserAction("AsyncServiceFacadeImpl.executeColorDepthService", search);
        Long serviceId = asyncServiceClient.invokeService("colorDepthObjectSearch",
                args, null, ImmutableMap.of());

        // Create a monitoring worker
        AsyncServiceMonitoringWorker executeWorker = new SearchMonitoringWorker(search, serviceId);
        executeWorker.executeWithEvents();

        return executeWorker;
    }

}
