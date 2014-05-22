package org.janelia.it.workstation.shared.workers;

import java.beans.PropertyChangeEvent;
import java.util.concurrent.Callable;

import org.janelia.it.workstation.api.entity_model.events.WorkerChangedEvent;
import org.janelia.it.workstation.api.entity_model.events.WorkerEndedEvent;
import org.janelia.it.workstation.api.entity_model.events.WorkerStartedEvent;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.shared.util.ConcurrentUtils;

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
    
    public BackgroundWorker(Callable<Void> success) {
        this.success = success;
    }

    @Override
    protected void hadSuccess() {
    }
    
    @Override
    protected void hadError(Throwable error) {
    }
    
    @Override
    protected void done() {
        super.done();
        ModelMgr.getModelMgr().postOnEventBus(new WorkerEndedEvent(this));
    }

    public abstract String getName();

    protected void setStatus(String status) {
        this.status = status;
        ModelMgr.getModelMgr().postOnEventBus(new WorkerChangedEvent(this));
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        super.propertyChange(e);
        ModelMgr.getModelMgr().postOnEventBus(new WorkerChangedEvent(this));
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
            hadError(e);
        }
    }
    
    /**
     * Same as execute(), except throws events on the ModelMgr's EventBus.
     */
    public void executeWithEvents() {
        execute();
        ModelMgr.getModelMgr().postOnEventBus(new WorkerStartedEvent(this));
    }
}
