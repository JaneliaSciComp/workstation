package org.janelia.it.workstation.browser.events.workers;

import org.janelia.it.workstation.browser.workers.BackgroundWorker;

/**
 * Worker has started.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class WorkerStartedEvent extends WorkerEvent {
    public WorkerStartedEvent(BackgroundWorker worker) {
        super(worker);
    }
}
