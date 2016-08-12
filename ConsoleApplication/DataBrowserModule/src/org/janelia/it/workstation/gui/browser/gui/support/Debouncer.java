package org.janelia.it.workstation.gui.browser.gui.support;

import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A "debouncer" or "deduper" for async method calls. When someone calls your 
 * method, queue the callback using queue(). When you're done, call either 
 * success() or failure() depending on the outcome. 
 * 
 * If queue() returns false, you should exit your method immediately, knowing 
 * that the operation is already in progress, and your callback has been 
 * queued appropriately. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Debouncer {

    private final Logger log = LoggerFactory.getLogger(Debouncer.class);
    
    private final AtomicBoolean operationInProgress = new AtomicBoolean(false);
    private final Queue<Callable<Void>> callbacks = new ConcurrentLinkedQueue<>();
    private final Queue<Map<String,Object>> parameterQueue = new ConcurrentLinkedQueue<>();

    public boolean queue() {
        return queue(null);
    }
    
    public synchronized boolean queue(Callable<Void> success) {
        if (success != null) {
            callbacks.add(success);
        }
        if (operationInProgress.getAndSet(true)) {
            return false;
        }
        return true;
    }

    public synchronized boolean queueWithParameters(Callable<Void> success, Map<String,Object> parameters) {
        if (success != null) {
            callbacks.add(success);
        }
        if (operationInProgress.getAndSet(true)) {
            if (parameters != null) {
                log.trace("Queueing with parameters: {}",parameters);
                parameterQueue.add(parameters);
            }
            return false;
        }
        return true;
    }

    public Map<String,Object> drainToLastParameters() {
        Map<String,Object> parameters = null;
        for (Iterator<Map<String,Object>> iterator = parameterQueue.iterator(); iterator.hasNext();) {
            parameters = iterator.next();
            iterator.remove();
        }
        log.trace("Returning last parameters: {}",parameters);
        return parameters;
    }

    public void success() {
        operationInProgress.set(false);
        executeCallBacks();
    }
    
    public void failure() {
        operationInProgress.set(false);
    }
    
    private synchronized void executeCallBacks() {
        for (Iterator<Callable<Void>> iterator = callbacks.iterator(); iterator.hasNext();) {
            try {
                iterator.next().call();
            }
            catch (Exception e) {
                log.error("Error executing callback", e);
            }
            iterator.remove();
        }
    }
    
}
