package org.janelia.it.workstation.gui.browser.events.workers;

import org.janelia.it.workstation.gui.browser.workers.BackgroundWorker;

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
