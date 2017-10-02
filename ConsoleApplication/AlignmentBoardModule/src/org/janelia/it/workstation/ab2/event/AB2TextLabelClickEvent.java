package org.janelia.it.workstation.ab2.event;

import org.janelia.it.workstation.ab2.actor.TextLabelActor;

public class AB2TextLabelClickEvent extends AB2Event {
    TextLabelActor textLabelActor;

    public AB2TextLabelClickEvent(TextLabelActor textLabelActor) {
        this.textLabelActor=textLabelActor;
    }

    public TextLabelActor getTextLabelActor() { return textLabelActor; }
}
