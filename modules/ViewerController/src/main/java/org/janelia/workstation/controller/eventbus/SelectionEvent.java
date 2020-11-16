package org.janelia.workstation.controller.eventbus;

import java.util.List;

public class SelectionEvent extends ViewerEvent {
    private List items;
    private boolean select = true;
    private boolean clear = false;

    public SelectionEvent(Object source, List items, boolean select, boolean clear) {
        super(source);
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
