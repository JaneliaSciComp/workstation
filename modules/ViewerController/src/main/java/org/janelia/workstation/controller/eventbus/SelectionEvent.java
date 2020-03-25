package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.DomainObject;

import java.util.List;

public class SelectionEvent {
    public enum Type {
        SELECT, DESELECT, CLEAR;
    };
    public enum Category {
        NEURON, VERTEX;
    };
    private SelectionEvent.Type type;
    private SelectionEvent.Category category;
    private List<DomainObject> items;

    public SelectionEvent() {
    }

    public SelectionEvent.Type getType() {
        return type;
    }

    public void setType(SelectionEvent.Type type) {
        this.type = type;
    }

    public SelectionEvent.Category getCategory() {
        return category;
    }

    public void setCategory(SelectionEvent.Category category) {
        this.category = category;
    }

    public List<DomainObject> getItems() {
        return items;
    }

    public void setItems(List<DomainObject> items) {
        this.items = items;
    }
}
