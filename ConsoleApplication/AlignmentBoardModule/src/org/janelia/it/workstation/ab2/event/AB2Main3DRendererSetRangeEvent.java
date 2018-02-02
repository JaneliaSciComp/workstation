package org.janelia.it.workstation.ab2.event;

public class AB2Main3DRendererSetRangeEvent extends AB2Event {
    private float r0;
    private float r1;

    public AB2Main3DRendererSetRangeEvent(float r0, float r1) {
        this.r0=r0;
        this.r1=r1;
    }

    public float getR0() {
        return r0;
    }

    public float getR1() {
        return r1;
    }
}
