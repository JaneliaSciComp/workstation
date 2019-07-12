package org.janelia.workstation.core.events.selection;

import java.util.ArrayList;
import java.util.List;

/**
 * An object was selected somewhere, either by the user or by some cascading process.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ObjectSelectionEvent<T> {

    private final Object sourceComponent;
    private final List<T> objects;
    private final boolean select;
    private final boolean clearAll;
    private final boolean isUserDriven;

    public ObjectSelectionEvent(Object source, List<? extends T> objects, boolean select, boolean clearAll, boolean isUserDriven) {
        this.sourceComponent = source;
        this.objects = new ArrayList<>(objects);
        this.select = select;
        this.clearAll = clearAll;
        this.isUserDriven = isUserDriven;
    }

    public Object getSourceComponent() {
        return sourceComponent;
    }

    public T getObjectIfSingle() {
        return objects.size()==1 ? objects.get(0) : null;
    }

    public List<T> getObjects() {
        return objects;
    }

    public boolean isSelect() {
        return select;
    }

    public boolean isClearAll() {
        return clearAll;
    }

    public boolean isUserDriven() {
        return isUserDriven;
    }

    @Override
    public String toString() {
        String s = sourceComponent == null ? null : sourceComponent.getClass().getSimpleName();
        return "ObjectSelectionEvent [sourceComponent=" + s + ", objects=" + objects
                + ", select=" + select + ", clearAll=" + clearAll + ", isUserDriven=" + isUserDriven + "]";
    }
}
