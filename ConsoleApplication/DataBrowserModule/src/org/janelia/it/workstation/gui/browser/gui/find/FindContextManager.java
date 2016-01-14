package org.janelia.it.workstation.gui.browser.gui.find;

import org.janelia.it.workstation.gui.browser.events.Events;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps track of the current find context.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FindContextManager {

    private static final Logger log = LoggerFactory.getLogger(FindContextManager.class);
    public static FindContextManager instance;
    
    private FindContextManager() {
    }
    
    public static FindContextManager getInstance() {
        if (instance==null) {
            instance = new FindContextManager();
            Events.getInstance().registerOnEventBus(instance);
        }
        return instance;
    }

    /* Manage the active context */
    
    private FindContext activeInstance;
    public void setCurrentContext(FindContext instance) {
        log.debug("Set current find context: "+instance);
        this.activeInstance = instance;
    }
    public FindContext getCurrentContext() {
        return activeInstance;
    }
}
