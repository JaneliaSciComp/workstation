package org.janelia.it.workstation.browser.workers;

import java.beans.PropertyChangeEvent;
import java.util.concurrent.Callable;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.browser.components.ProgressTopComponent;
import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.events.workers.WorkerChangedEvent;
import org.janelia.it.workstation.browser.events.workers.WorkerEndedEvent;
import org.janelia.it.workstation.browser.events.workers.WorkerStartedEvent;
import org.janelia.it.workstation.browser.util.ConcurrentUtils;

/**
 * A worker thread which can be monitored in the background.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class BackgroundWorker extends SimpleWorker {

    private String status;
    private Callable<Void> success;

    public BackgroundWorker() {
    }
    
    /**
     * The success callback is not something that is run automatically when the worker completes. 
     * Instead, it's something that can be triggered by the user if the worker was successful.
     * When the user triggers the callback (e.g. via a button press), the client should call 
     * runSuccessCallback() to invoke it. 
     * @param success
     */
    public BackgroundWorker(Callable<Void> success) {
        this.success = success;
    }

    /**
     * This method does nothing. The client must check getError() and deal with errors or success. 
     * Subclasses can choose to implement this to do something automatically when the worker is 
     * complete. 
     */
    @Override
    protected void hadSuccess() {
    }
    
    /**
     * This method does nothing. The client must check getError() and deal with the error somehow.
     */
    @Override
    protected void hadError(Throwable error) {
    }
    
    @Override
    protected void done() {
        super.done();
        Events.getInstance().postOnEventBus(new WorkerEndedEvent(this));
    }

    public abstract String getName();

    public void setStatus(String status) {
        this.status = status;
        Events.getInstance().postOnEventBus(new WorkerChangedEvent(this));
    }

    public void setFinalStatus(String status) {
        setStatus(status);
        setProgress(100);
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        super.propertyChange(e);
        Events.getInstance().postOnEventBus(new WorkerChangedEvent(this));
    }
    
    public String getStatus() {
        return status;
    }
    
    public Callable<Void> getSuccessCallback() {
        return success;
    }

    public void setSuccessCallback(Callable<Void> success) {
        this.success = success;
    }

    public void runSuccessCallback() {
        try {
            ConcurrentUtils.invoke(getSuccessCallback());
        }
        catch (Exception e) {
            FrameworkImplProvider.handleException("Problem invoking success callback", e);
        }
    }
    
    /**
     * Same as execute(), except throws events on the EventBus.
     */
    public void executeWithEvents() {
        ProgressTopComponent.ensureActive();
        Events.getInstance().postOnEventBus(new WorkerStartedEvent(BackgroundWorker.this));
        execute();
    }
}
