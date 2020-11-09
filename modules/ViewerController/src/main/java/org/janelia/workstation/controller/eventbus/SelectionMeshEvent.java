package org.janelia.workstation.controller.eventbus;

import java.util.List;

public class SelectionMeshEvent extends SelectionEvent {
    public SelectionMeshEvent(List items, boolean select, boolean clear) {
        super(items, select, clear);
    }
}
