package org.janelia.it.workstation.browser.events.workers;

import org.janelia.it.workstation.browser.workers.BackgroundWorker;

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
