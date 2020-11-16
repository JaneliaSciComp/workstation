package org.janelia.workstation.controller.eventbus;

import java.util.List;

public class SelectionNeuronsEvent extends SelectionEvent {
    public SelectionNeuronsEvent(Object source,
                                 List items,
                                 boolean select, boolean clear) {
        super(source, items, select, clear);
    }
}
