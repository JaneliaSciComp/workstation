package org.janelia.it.workstation.api.entity_model.events;

/**
 * Worker state has changed. For example, the status or progress may have been updated.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class WorkerChangedEvent extends WorkerEvent {
    public WorkerChangedEvent(org.janelia.it.workstation.shared.workers.BackgroundWorker worker) {
        super(worker);
    }
}
