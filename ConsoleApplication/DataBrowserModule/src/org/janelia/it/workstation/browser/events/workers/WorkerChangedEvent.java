package org.janelia.it.workstation.browser.events.workers;

import org.janelia.it.workstation.browser.workers.BackgroundWorker;

/**
 * Worker state has changed. For example, the status or progress may have been updated.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class WorkerChangedEvent extends WorkerEvent {
    public WorkerChangedEvent(BackgroundWorker worker) {
        super(worker);
    }
}
