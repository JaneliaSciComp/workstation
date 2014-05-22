package org.janelia.it.workstation.shared.workers;

import org.janelia.it.workstation.api.entity_model.events.WorkerChangedEvent;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * A background worker thread which monitors a server-side task. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TaskMonitoringWorker extends BackgroundWorker {

    protected static final int REFRESH_DELAY_MS = 2000;
    
    private Long taskId;
    private Task task;

    public TaskMonitoringWorker() {
    }
    
    public TaskMonitoringWorker(Long taskId) {
        this.taskId = taskId;
    }
    
    @Override
    protected void doStuff() throws Exception {
        
        while (true) {
            this.task = org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().getTaskById(taskId);
            setStatus(task.getLastEvent().getDescription());
            org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().postOnEventBus(new WorkerChangedEvent(this));
            
            if (task==null) {
                throw new IllegalStateException("Task does not exist: "+taskId);
            }

            if (task.isDone()) {
                // Check for errors
                if (task.getLastEvent().getEventType().equals(Event.ERROR_EVENT)) {
                    throw new ServiceException(task.getLastEvent().getDescription());
                }
                return;
            }
            
            try {
                Thread.sleep(REFRESH_DELAY_MS);
            }
            catch (InterruptedException e) {
                hadError(e);
                return;
            }
        }
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public Task getTask() {
        return task;
    }
    
    public String getName() {
        if (task==null) return "None";
        return task.getDisplayName();
    }
}
