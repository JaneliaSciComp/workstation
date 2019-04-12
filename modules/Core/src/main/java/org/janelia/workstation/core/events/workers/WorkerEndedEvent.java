package org.janelia.workstation.core.events.workers;

import org.janelia.workstation.core.workers.BackgroundWorker;

/**
 * Worker has ended.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class WorkerEndedEvent extends WorkerEvent {
    public WorkerEndedEvent(BackgroundWorker worker) {
        super(worker);
    }
}
