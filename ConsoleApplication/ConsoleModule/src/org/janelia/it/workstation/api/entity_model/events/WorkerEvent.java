package org.janelia.it.workstation.api.entity_model.events;

/**
 * A worker has changed in some way.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class WorkerEvent {
    private org.janelia.it.workstation.shared.workers.BackgroundWorker worker;
    public WorkerEvent(org.janelia.it.workstation.shared.workers.BackgroundWorker worker) {
        this.worker = worker;
    }
    public org.janelia.it.workstation.shared.workers.BackgroundWorker getWorker() {
        return worker;
    }
}
