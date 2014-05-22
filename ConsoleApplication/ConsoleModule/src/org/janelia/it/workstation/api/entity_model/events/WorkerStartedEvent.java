package org.janelia.it.workstation.api.entity_model.events;

/**
 * Worker has started.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class WorkerStartedEvent extends WorkerEvent {
    public WorkerStartedEvent(org.janelia.it.workstation.shared.workers.BackgroundWorker worker) {
        super(worker);
    }
}
