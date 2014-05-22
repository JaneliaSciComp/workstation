package org.janelia.it.workstation.gui.util.swing_models;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CollectionJListModel extends AbstractListModel {

    private List delegate;
    private boolean dirtyFlag = false;

    //----------------------------------IMPLEMENTATION of AbstractListModel

    /**
     * Constructor to seed with whole collection.
     */
    public CollectionJListModel(Collection seedCollection) {
        delegate = new ArrayList();
        delegate.addAll(seedCollection);
    } // End method

    /**
     * Returns size of delegated collection.
     */
    public synchronized int getSize() {
        return delegate.size();
    } // End method

    /**
     * Returns required element from collection.
     */
    public synchronized Object getElementAt(int index) {
        if (withinRange(index)) return delegate.get(index);
        else return null;
    } // End method

    //----------------------------------OTHER INTERFACE METHODS

    /**
     * Finds last element in delegated collection.
     */
    public synchronized Object findLast() {
        if (delegate.size() > 0) return delegate.get(delegate.size() - 1);
        else return null;
    } // End method

    /**
     * Takes item out at index.
     *
     * @param index where to remove.
     */
    public synchronized void remove(int index) {
        if (withinRange(index)) {
            delegate.remove(index);
            fireIntervalRemoved(this, index, index);
            dirtyFlag = true;
        } // In the collection
    } // End method

    /**
     * Takes item given out of list.
     *
     * @param value what to remove.
     */
    public synchronized void remove(Object value) {
        if (delegate.contains(value)) {
            int index = delegate.indexOf(value);
            remove(index);
        } // Has value.
    } // End method

    /**
     * Adds the specified component to the end of this list.
     *
     * @param value the component to be added.
     */
    public synchronized void add(Object value) {
        int index = delegate.size();
        delegate.add(value);
        fireIntervalAdded(this, index, index);
        dirtyFlag = true;
    } // End method

    /**
     * Retrieves whole collection from here.
     */
    public synchronized Collection getCollection() {
        return delegate;
    } // End method

    /**
     * Retrieves collection, but lets user assume it is a list, and lets
     * them treat it as an ordered collection.
     */
    public synchronized List getList() {
        return delegate;
    } // End method

    /**
     * Tells if any setters were invoked.
     */
    public boolean isModified() {
        return dirtyFlag;
    }

    //----------------------------------HELPER METHODS

    /**
     * Reports whether the index is realistically in the collection.
     */
    private boolean withinRange(int index) {
        if (index < 0) return false;
        if (index >= delegate.size()) return false;
        return true;
    } // End method

} // End class
