package org.janelia.workstation.controller.eventbus;

import java.util.List;

public class SelectionAnnotationEvent extends SelectionEvent {
    public SelectionAnnotationEvent(List items, boolean select, boolean clear) {
        super(items, select, clear);
    }
}
