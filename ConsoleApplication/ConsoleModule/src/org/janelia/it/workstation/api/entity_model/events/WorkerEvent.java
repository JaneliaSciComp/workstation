package org.janelia.it.workstation.api.entity_model.events;

import org.janelia.it.workstation.shared.workers.BackgroundWorker;

/**
 * A worker has changed in some way.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class WorkerEvent {
    private BackgroundWorker worker;
    public WorkerEvent(BackgroundWorker worker) {
        this.worker = worker;
    }
    public BackgroundWorker getWorker() {
        return worker;
    }
}
