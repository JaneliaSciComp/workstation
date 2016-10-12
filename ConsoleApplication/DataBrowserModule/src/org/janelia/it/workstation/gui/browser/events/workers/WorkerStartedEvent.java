package org.janelia.it.workstation.gui.browser.events.workers;

import org.janelia.it.workstation.gui.browser.workers.BackgroundWorker;

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
