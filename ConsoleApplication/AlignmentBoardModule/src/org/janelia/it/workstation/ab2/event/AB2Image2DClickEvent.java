package org.janelia.it.workstation.ab2.event;

import org.janelia.it.workstation.ab2.actor.Image2DActor;

public class AB2Image2DClickEvent extends AB2Event {
    Image2DActor image2DActor;

    public AB2Image2DClickEvent(Image2DActor image2DActor) {
        this.image2DActor=image2DActor;
    }

    public Image2DActor getPickSquareActor() { return image2DActor; }
}

