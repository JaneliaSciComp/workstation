package org.janelia.workstation.browser.gui.colordepth;

import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.workers.AsyncServiceMonitoringWorker;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.model.domain.gui.cdmip.ColorDepthSearch;

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
        // Ensure that search object get refreshed with results once the search is complete
        SimpleWorker.runInBackground(() -> {
           DomainMgr.getDomainMgr().getModel().invalidate(search);
        });
    }
}