package org.janelia.workstation.controller.eventbus;

import java.util.List;

public class SelectionAnnotationEvent extends SelectionEvent {
    public SelectionAnnotationEvent(Object source,
                                    List items,
                                    boolean select, boolean clear) {
        super(source, items, select, clear);
    }
}
