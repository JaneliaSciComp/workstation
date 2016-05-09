package org.janelia.it.workstation.gui.browser.events;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import java.awt.EventQueue;
import java.util.concurrent.Executor;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Global event bus singleton. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Events {

    private static final Logger log = LoggerFactory.getLogger(Events.class);
    
    private static final Events singleton = new Events();
    
    public static Events getInstance() {
        return singleton;
    }
    
    private final EventBus eventBus;
            
    private Events() {
        this.eventBus = new AsyncEventBus("awt", new Executor() {
            @Override
            public void execute(Runnable cmd) {
                if (EventQueue.isDispatchThread()) {
                    cmd.run();
                }
                else {
                    // TODO: this should queue the command on a queue that is aware of entity invalidation, 
                    // and does not generate other events for an entity if an invalidation is coming. 
                    // This will elimiante the "Instance mismatch" issues that we sometimes have.
                    EventQueue.invokeLater(cmd);
                }
            }
        });
    }
    
    public void registerOnEventBus(Object object) {
        log.debug("Registering: {}",object);
        try {
            synchronized (Events.class) {
                eventBus.register(object);
            }
        }
        catch (IllegalArgumentException e) {
            log.warn("Cannot register object on event bus: {}", e.getMessage());
        }
    }

    public void unregisterOnEventBus(Object object) {
        log.debug("Unregistering: {}",object);
        try {
            synchronized (Events.class) {
                eventBus.unregister(object);
            }
        }
        catch (IllegalArgumentException e) {
            log.warn("Cannot unregister object on event bus: {}",e.getMessage());
        }
    }

    public void postOnEventBus(Object object) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Post on event bus from " + 
                        Thread.currentThread().getClass().getClassLoader() + "/" + 
                        Thread.currentThread().getContextClassLoader() + " in thread " + 
                        Thread.currentThread());
            }
            synchronized (ModelMgr.class) {
                eventBus.post(object);
            }
        }
        catch (IllegalArgumentException e) {
            log.warn("Cannot post event on event bus: {}",e.getMessage());
        }
    }
}
