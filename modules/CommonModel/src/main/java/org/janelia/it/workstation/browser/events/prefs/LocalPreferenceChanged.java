package org.janelia.it.workstation.browser.events.prefs;

/**
 * Event indicating that a local preference has changed.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class LocalPreferenceChanged {
    
    private Object key;
    private Object oldValue;
    private Object newValue;
    
    public LocalPreferenceChanged(Object key, Object oldValue, Object newValue) {
        super();
        this.key = key;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public Object getKey() {
        return key;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }
}
