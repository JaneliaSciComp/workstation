package org.janelia.workstation.controller.eventbus;

import java.util.List;

public class SelectionMeshEvent extends SelectionEvent {
    public SelectionMeshEvent(Object source,
                              List items,
                              boolean select, boolean clear) {
        super(source, items, select, clear);
    }
}
