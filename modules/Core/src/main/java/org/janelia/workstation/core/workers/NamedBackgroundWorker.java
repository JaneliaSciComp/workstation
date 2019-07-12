package org.janelia.workstation.core.workers;

import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.workers.WorkerChangedEvent;

public abstract class NamedBackgroundWorker extends BackgroundWorker {
    
    private String name;

    public void setName(String name) {
        this.name = name;
        if (isEmitEvents()) {
            Events.getInstance().postOnEventBus(new WorkerChangedEvent(this));
        }
    }
    
    @Override
    public String getName() {
        return name;
    }
}