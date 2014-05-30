package org.janelia.it.workstation.api.entity_model.events;

import org.janelia.it.workstation.shared.workers.BackgroundWorker;

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
