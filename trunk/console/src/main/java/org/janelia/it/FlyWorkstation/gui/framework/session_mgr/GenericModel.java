package org.janelia.it.FlyWorkstation.gui.framework.session_mgr;

import org.janelia.it.FlyWorkstation.shared.util.EmptyIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * The GenericModel is a generic observer model for the components of the browser.
 * Changes to any of these elements will broadcast events notifing all listeners of
 * the change.
 * <p/>
 * Initially written by: Todd Safford
 */

public abstract class GenericModel {
    
    private static final Logger log = LoggerFactory.getLogger(GenericModel.class);
    
    protected ArrayList<GenericModelListener> modelListeners = new ArrayList<GenericModelListener>();
    protected TreeMap modelProperties;

    public GenericModel() {
        modelProperties = new TreeMap();
        modelListeners = new ArrayList<GenericModelListener>();
    }  //Constructor can only be called within the package


    public void addModelListener(GenericModelListener modelListener) {
        if (!modelListeners.contains(modelListener)) modelListeners.add(modelListener);
    }

    public void removeModelListener(GenericModelListener modelListener) {
        modelListeners.remove(modelListener);
    }

    /**
     * @return The previous value of the this key or null
     */
    public Object setModelProperty(Object key, Object newValue) {
        if (modelProperties == null) modelProperties = new TreeMap();
        Object oldValue = modelProperties.put(key, newValue);
        fireModelPropertyChangeEvent(key, oldValue, newValue);
        return oldValue;
    }

    public Object getModelProperty(Object key) {
        if (modelProperties == null) return null;
        return modelProperties.get(key);
    }

    public void removeModelProperty(Object key){
        modelProperties.remove(key);
        fireModelPropertyChangeEvent(key, null, null);
    }

    int sizeofProperties() {
        if (!modelProperties.isEmpty()) return modelProperties.size();
        else return 0;
    }

    public Iterator getModelPropertyKeys() {
        if (modelProperties == null) return new EmptyIterator();
        return modelProperties.keySet().iterator();
    }

    public TreeMap getModelProperties() {
        return modelProperties;
    }

    protected void setModelProperties(TreeMap modelProperties) {
        this.modelProperties = modelProperties;
    }

    private void fireModelPropertyChangeEvent(Object key, Object oldValue, Object newValue) {
        log.debug("Firing model property change for "+key+": valueDiffers?="+(newValue!=null && !newValue.equals(oldValue)));
        for (GenericModelListener modelListener : modelListeners) {
            log.debug("  telling "+modelListener);
            modelListener.modelPropertyChanged(key, oldValue, newValue);
        }
    }

}
