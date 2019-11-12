package org.janelia.workstation.core.workers;

import java.util.concurrent.Callable;

import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.workers.AsyncServiceMonitoringWorker;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.model.domain.gui.cdmip.ColorDepthSearch;
import org.janelia.workstation.integration.util.FrameworkAccess;

/**
 * Monitor a color depth search being executed on the backend.
 */
public class SearchMonitoringWorker extends AsyncServiceMonitoringWorker {
    
    private ColorDepthSearch search;

    public SearchMonitoringWorker(ColorDepthSearch search, Long serviceId) {
        super(serviceId);
        this.search = search;
    }

    public ColorDepthSearch getSearch() {
        return search;
    }

    @Override
    public String getName() {
        return "Executing "+search.getName();
    }

    protected void completed() {
        forceInvalidate();
    }

    @Override
    public Callable<Void> getSuccessCallback() {
        return () -> {
            // Refresh and load the search which is completed
            forceInvalidate();
            return null;
        };
    }

    private void forceInvalidate() {
        SimpleWorker.runInBackground(() -> {
            try {
                DomainMgr.getDomainMgr().getModel().invalidate(search);
            }
            catch (Exception ex) {
                FrameworkAccess.handleExceptionQuietly(ex);
            }
        });
    }
}