package org.janelia.it.workstation.api.entity_model.events;

import org.janelia.it.workstation.shared.workers.BackgroundWorker;

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
