package org.janelia.workstation.controller.eventbus;

import org.janelia.workstation.controller.AnnotationCategory;
import java.util.List;

public class SelectionEvent {
    private List items;
    private boolean select = true;
    private boolean clear = false;

    public SelectionEvent(List items, boolean select, boolean clear) {
        this.items = items;
        this.select = select;
        this.clear = clear;
    }

    public List getItems() {
        return items;
    }
    public boolean isSelect() {
        return select;
    }
    public boolean isClear() {
        return clear;
    }
}
