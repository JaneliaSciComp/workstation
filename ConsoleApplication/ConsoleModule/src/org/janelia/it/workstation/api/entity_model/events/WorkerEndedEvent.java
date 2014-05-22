package org.janelia.it.workstation.api.entity_model.events;

/**
 * Worker has ended.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class WorkerEndedEvent extends WorkerEvent {
    public WorkerEndedEvent(org.janelia.it.workstation.shared.workers.BackgroundWorker worker) {
        super(worker);
    }
}
