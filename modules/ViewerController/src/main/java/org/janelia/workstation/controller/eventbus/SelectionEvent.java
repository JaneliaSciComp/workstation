package org.janelia.workstation.controller.eventbus;

import org.janelia.workstation.controller.AnnotationCategory;
import java.util.List;

public class SelectionEvent {
    public enum Type {
        SELECT, DESELECT, CLEAR;
    };

    private Type type;
    private AnnotationCategory category;
    private List items;

    public SelectionEvent(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public AnnotationCategory getCategory() {
        return category;
    }

    public void setCategory(AnnotationCategory category) {
        this.category = category;
    }

    public List getItems() {
        return items;
    }

    public void setItems(List items) {
        this.items = items;
    }
}
