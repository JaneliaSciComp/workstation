package org.janelia.workstation.controller.eventbus;

import org.janelia.workstation.controller.AnnotationCategory;
import java.util.List;

public class SelectionEvent {
    private List items;
    private boolean select = true;
    private boolean clear = false;

    public List getItems() {
        return items;
    }

    public void setItems(List items) {
        this.items = items;
    }

    public boolean isSelect() {
        return select;
    }

    public void setSelect(boolean select) {
        this.select = select;
    }

    public boolean isClear() {
        return clear;
    }

    public void setClear(boolean clear) {
        this.clear = clear;
    }
}
