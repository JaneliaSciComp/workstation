package org.janelia.workstation.core.events.workers;

import org.janelia.workstation.core.workers.BackgroundWorker;

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
