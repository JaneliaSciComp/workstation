package org.janelia.it.workstation.gui.browser.gui.find;

import java.util.HashSet;
import java.util.Set;

import org.janelia.it.workstation.gui.browser.events.Events;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps track of find contexts, so that they are ready whenever the user 
 * invokes the Find operation. 
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
    
    private Set<FindContext> activeContexts = new HashSet<>();
    
    public void activateContext(FindContext context) {
        log.debug("Activate find context: "+context);
        this.activeContexts.add(context);
    }
    
    public void deactivateContext(FindContext context) {
        log.debug("Deactivate find context: "+context);
        this.activeContexts.remove(context);
    }
    
    public Set<FindContext> getActiveContexts() {
        return activeContexts;
    }
}
