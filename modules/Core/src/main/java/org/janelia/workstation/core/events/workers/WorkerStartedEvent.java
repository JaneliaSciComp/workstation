package org.janelia.workstation.core.events.workers;

import org.janelia.workstation.core.workers.BackgroundWorker;

/**
 * Worker has started.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class WorkerStartedEvent extends WorkerEvent {

    boolean showProgressMonitor;

    public WorkerStartedEvent(BackgroundWorker worker, boolean showProgressMonitor) {
        super(worker);
        this.showProgressMonitor = showProgressMonitor;
    }

    public boolean isShowProgressMonitor() {
        return showProgressMonitor;
    }
}
