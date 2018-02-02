package org.janelia.it.workstation.ab2.event;

public class AB2Sample3DImageLoadedEvent extends AB2Event {

    byte[] data;

    public AB2Sample3DImageLoadedEvent(byte[] data) {
        this.data=data;
    }

    public byte[] getData() { return data; }

    public void clearData() { data=null; }
}
