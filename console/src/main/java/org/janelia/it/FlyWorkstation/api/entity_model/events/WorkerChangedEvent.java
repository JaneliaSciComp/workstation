package org.janelia.it.FlyWorkstation.api.entity_model.events;

import org.janelia.it.FlyWorkstation.shared.workers.BackgroundWorker;

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
