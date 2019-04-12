package org.janelia.workstation.core.api.facade.interfaces;

import org.janelia.it.jacs.model.tasks.Task;

public interface LegacyFacade {

    Task saveOrUpdateTask(Task task) throws Exception;

    Task getTaskById(Long taskId) throws Exception;

    void submitJob(String processDefName, Long taskId) throws Exception;

    void dispatchJob(String processDefName, Long taskId) throws Exception;

}
