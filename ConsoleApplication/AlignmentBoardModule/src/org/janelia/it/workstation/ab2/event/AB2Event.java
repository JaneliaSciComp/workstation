package org.janelia.it.workstation.ab2.event;

import java.util.Date;

public abstract class AB2Event {
    protected long timestamp;
    private int actorPickId=0;

    public AB2Event() {
        timestamp=new Date().getTime();
    }

    public long getTimestamp() { return timestamp; }

    public int getActorPickId() {
        return actorPickId;
    }

    public void setActorPickId(int actorPickId) {
        this.actorPickId = actorPickId;
    }
}
