package org.janelia.workstation.core.workers;

import java.beans.PropertyChangeEvent;
import java.util.concurrent.Callable;


import org.apache.commons.lang3.StringUtils;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.workers.WorkerChangedEvent;
import org.janelia.workstation.core.events.workers.WorkerEndedEvent;
import org.janelia.workstation.core.events.workers.WorkerStartedEvent;
import org.janelia.workstation.core.util.ConcurrentUtils;
import org.janelia.workstation.core.util.StringUtilsExtra;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A worker thread which can be monitored in the background.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class BackgroundWorker extends SimpleWorker {

    private static final Logger log = LoggerFactory.getLogger(BackgroundWorker.class);

    protected String status;
    protected Callable<Void> success;
    protected boolean emitEvents = false;
    protected boolean showProgressMonitor = true;

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
        if (emitEvents) {
            Events.getInstance().postOnEventBus(new WorkerEndedEvent(this, showProgressMonitor));
        }
    }

    public abstract String getName();

    public void setStatus(String status) {
        if (StringUtilsExtra.areEqual(status, this.status)) return;
        this.status = status;

        log.debug("Worker '{}' changed status to: {}", getName(), status);

        if (emitEvents) {
            Events.getInstance().postOnEventBus(new WorkerChangedEvent(this));
        }
    }

    public void setFinalStatus(String status) {
        setStatus(status);
        setProgress(100);
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        super.propertyChange(e);
        if (emitEvents) {
            Events.getInstance().postOnEventBus(new WorkerChangedEvent(this));
        }
    }
    
    public String getStatus() {
        return status;
    }

    public boolean isEmitEvents() {
        return emitEvents;
    }

    public boolean isShowProgressMonitor() {
        return showProgressMonitor;
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
            FrameworkAccess.handleException("Problem invoking success callback", e);
        }
    }

    /**
     * Same as execute(), except throws events on the EventBus.
     */
    public void executeWithEvents() {
        executeWithEvents(true);
    }

    /**
     * Same as execute(), except throws events on the EventBus.
     * @param showProgressMonitor when the worker is started and finished, should the progress meter panel popup to the
     *                            user, so that they have a visual indication of the task progress?
     */
    public void executeWithEvents(boolean showProgressMonitor) {
        this.emitEvents = true;
        this.showProgressMonitor = showProgressMonitor;
        Events.getInstance().postOnEventBus(new WorkerStartedEvent(this, showProgressMonitor));
        execute();
    }
}
