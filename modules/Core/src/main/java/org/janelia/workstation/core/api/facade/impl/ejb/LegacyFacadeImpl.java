package org.janelia.workstation.core.api.facade.impl.ejb;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.workstation.core.api.facade.interfaces.LegacyFacade;

public class LegacyFacadeImpl implements LegacyFacade {
    
    @Override
    public Task saveOrUpdateTask(Task task) throws Exception {
        if (task == null) throw new IllegalArgumentException("Task may not be null");
        return EJBFactory.getRemoteComputeBean().saveOrUpdateTask(task);
    }

    @Override
    public Task getTaskById(Long taskId) throws Exception {
        if (taskId == null) throw new IllegalArgumentException("Task id may not be null");
        return EJBFactory.getRemoteComputeBean().getTaskById(taskId);
    }

    @Override
    public void submitJob(String processDefName, Long taskId) throws Exception {
        if (taskId == null) throw new IllegalArgumentException("Task id may not be null");
        EJBFactory.getRemoteComputeBean().submitJob(processDefName, taskId);
    }

    @Override
    public void dispatchJob(String processDefName, Long taskId) throws Exception {
        if (taskId == null) throw new IllegalArgumentException("Task id may not be null");
        EJBFactory.getRemoteComputeBean().dispatchJob(processDefName, taskId);
    }

    public static class EJBLookupException extends Exception {
        public EJBLookupException(String message) {
            super(message);
        }
    }
}
