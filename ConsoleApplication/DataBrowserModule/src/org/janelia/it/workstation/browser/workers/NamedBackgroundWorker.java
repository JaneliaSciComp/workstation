package org.janelia.it.workstation.browser.workers;

import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.events.workers.WorkerChangedEvent;

public abstract class NamedBackgroundWorker extends BackgroundWorker {
    
    private String name;

    public void setName(String name) {
        this.name = name;
        Events.getInstance().postOnEventBus(new WorkerChangedEvent(this));
    }
    
    @Override
    public String getName() {
        return name;
    }
}