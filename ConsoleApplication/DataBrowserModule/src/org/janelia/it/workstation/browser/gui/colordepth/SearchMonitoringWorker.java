package org.janelia.it.workstation.browser.gui.colordepth;

import org.janelia.it.workstation.browser.workers.AsyncServiceMonitoringWorker;
import org.janelia.model.domain.gui.colordepth.ColorDepthSearch;

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

    @Override
    protected boolean isOpenProgressMonitor() {
        return false;
    }
}