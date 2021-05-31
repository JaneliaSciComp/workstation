package org.janelia.workstation.core.events.selection;

import org.janelia.model.util.MapFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A generic selection model which tracks the selection of objects of type T, via unique identifiers of type S. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class SelectionModel<T,S> {
    
    private static final Logger log = LoggerFactory.getLogger(SelectionModel.class);

    // This is used mainly to improve performance of contains() over a normal list, while also providing a sorted list and allowing access to the last item.
    private List<T> selectedObjects = new ArrayList<>();
    private MapFacade<S, T> selected = new MapFacade<S, T>(selectedObjects) {
        @Override
        public S getKey(T object) {
            return getId(object);
        }
    };

    private Object source;
    
    /**
     * Returns the source component that is generating selections.
     * @return
     */
    public final Object getSource() {
        return source;
    }
    
    /**
     * Set the source component that is generating selections.
     * @param source
     */
    public final void setSource(Object source) {
        this.source = source;
    }

    /**
     * Select the given object, optionally clearing all other selections first. 
     * @param object
     * @param clearAll
     */
    public final void select(T object, boolean clearAll, boolean isUserDriven) {
        select(Collections.singletonList(object), clearAll, isUserDriven);
    }
    
    /**
     * Select the given objects, optionally clearing all other selections first. 
     * @param objects
     * @param clearAll
     */
    public final void select(List<T> objects, boolean clearAll, boolean isUserDriven) {
        if (objects.size()==1) {
            log.trace("{} - select(objects={}, clearAll={}, isUserDriven={})",getClass().getSimpleName(),objects,clearAll,isUserDriven);
        }
        else {
            log.trace("{} - select(objects.size={}, clearAll={}, isUserDriven={})",getClass().getSimpleName(),objects.size(),clearAll,isUserDriven);    
        }
        
        boolean clear = clearAll;
        for(T object : objects) {
            S id = getId(object);
            if (!isCorrectlySelected(id, clear)) {
                if (clear) {
                    selected.clear();
                }
                selected.put(id, object);
            }
            clear = false;
        }
        log.trace("{} - {} selected", getClass().getSimpleName(), selected.size());
        selectionChanged(objects, true, clearAll, isUserDriven);
    }
    
    private boolean isCorrectlySelected(S id, boolean clearAll) {
        if (selected.containsKey(id)) {
            if (clearAll) {
                if (selected.size()==1) {
                    // Already single selected
                    return true;
                }
                else {
                    // Multi-selected, but needs to be single selected
                    return false;
                }
            }
            else {
                // Already multi-selected
                return true;
            }
        }
        // Not selected
        return false;
    }

    /**
     * De-select the given object.
     * @param object
     */
    public final void deselect(T object, boolean isUserDriven) {
        if (object!=null) {
            deselect(Collections.singletonList(object), isUserDriven);
        }
    }
    
    /**
     * De-select the given objects.
     * @param objects
     */
    public final void deselect(List<T> objects, boolean isUserDriven) {
        if (objects.size()==1) {
            log.trace("{} - deselect(objects={}, isUserDriven={})",getClass().getSimpleName(),objects,isUserDriven);
        }
        else {
            log.trace("{} - deselect(objects.size={}, isUserDriven={})",getClass().getSimpleName(),objects.size(),isUserDriven);
        }
        
        for(T object : objects) {
            if (object != null) {
                S id = getId(object);
                selected.remove(id);
            }
        }
        log.trace("{} - {} selected", getClass().getSimpleName(), selected.size());
        selectionChanged(objects, false, false, isUserDriven);
    }

    /**
     * Clear all objects from the model. This resets the model, but does NOT call selectionChanged, since it
     * is assumed that no one cares at this point.
     */
    public void reset() {
        selected.clear();
    }
    
    /**
     * Sub-classes can implement this method to do something when an object's selection changes. 
     * @param objects
     * @param select
     * @param clearAll
     * @param isUserDriven
     */
    protected abstract void selectionChanged(List<T> objects, boolean select, boolean clearAll, boolean isUserDriven);
    
    /**
     * Sub-classes must implement this method to return an identifier for the given object.
     * @param object a object of the generic type T
     * @return unique identifier of type S for the given object
     */
    public abstract S getId(T object);
    
    /**
     * Returns a list of currently selected ids, in the other they were selected. 
     * @return
     */
    public final List<S> getSelectedIds() {
        return new ArrayList<>(selected.keySet());
    }
    
    /**
     * Returns true if the given object is currently selected in this model.
     * @param id
     * @return
     */
    public final boolean isSelected(S id) {
        return selected.containsKey(id);
    }

    /**
     * Returns true if the given object is currently selected in this model.
     * @param object
     * @return
     */
    public final boolean isObjectSelected(T object) {
        return selected.containsValue(object);
    }

    /**
     * Returns the last item that was selected, not null if nothing is currently selected.
     * @return
     */
    public final T getLastSelectedObject() {
        if (selected.isEmpty()) return null;
        return selectedObjects.get(selectedObjects.size() - 1);
    }

    /**
     * Returns the last item that was selected, not null if nothing is currently selected.
     * @return
     */
    public final S getLastSelectedId() {
        if (selected.isEmpty()) return null;
        T object = selectedObjects.get(selectedObjects.size()-1);
        return getId(object);
    }

    public List<T> getObjects() {
        return selectedObjects;
    }
}
