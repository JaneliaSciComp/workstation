package org.janelia.it.workstation.ab2.event;

import java.util.Date;

public abstract class AB2Event {
    protected long timestamp;

    public AB2Event() {
        timestamp=new Date().getTime();
    }

    public long getTimestamp() { return timestamp; }
}
