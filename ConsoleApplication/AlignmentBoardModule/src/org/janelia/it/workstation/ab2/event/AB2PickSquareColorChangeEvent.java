package org.janelia.it.workstation.ab2.event;

import org.janelia.it.workstation.ab2.actor.PickSquareActor;

public class AB2PickSquareColorChangeEvent extends AB2Event {

    PickSquareActor pickSquareActor;

    public AB2PickSquareColorChangeEvent(PickSquareActor pickSquareActor) {
        this.pickSquareActor=pickSquareActor;
    }

    public PickSquareActor getPickSquareActor() { return pickSquareActor; }
}
