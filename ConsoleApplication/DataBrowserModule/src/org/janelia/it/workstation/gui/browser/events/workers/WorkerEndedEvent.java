package org.janelia.it.workstation.gui.browser.events.workers;

import org.janelia.it.workstation.gui.browser.workers.BackgroundWorker;

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
