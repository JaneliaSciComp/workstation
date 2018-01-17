package org.janelia.it.workstation.ab2.event;

public class AB2MainMessageEvent extends AB2Event {
    String message;

    public AB2MainMessageEvent(String message) {
        this.message=message;
    }

    public String getMessage() { return message; }
}
