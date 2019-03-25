package org.janelia.it.workstation.browser.events.selection;

import java.util.List;

public abstract class ChildSelectionModel<T,S> extends SelectionModel<T,S> {

    private Object parentObject;
    
    public Object getParentObject() {
        return parentObject;
    }

    public void setParentObject(Object parentObject) {
        this.parentObject = parentObject;
    }

    protected void selectionChanged(List<T> objects, boolean select, boolean clearAll, boolean isUserDriven) {
    }

    public abstract S getId(T object);
    
}
