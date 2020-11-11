package org.janelia.workstation.controller.eventbus;

import java.util.List;

public class SelectionNeuronsEvent extends SelectionEvent {
    public SelectionNeuronsEvent(List items, boolean select, boolean clear) {
        super(items, select, clear);
    }
}
