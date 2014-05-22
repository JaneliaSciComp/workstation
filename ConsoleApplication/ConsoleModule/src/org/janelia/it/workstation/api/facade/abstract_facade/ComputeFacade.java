package org.janelia.it.workstation.api.facade.abstract_facade;

import java.util.List;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Subject;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 8/8/11
 * Time: 11:07 AM
 */
public interface ComputeFacade {

    public Task saveOrUpdateTask(Task task) throws Exception;

    public Task getTaskById(Long taskId) throws Exception;
    
    public void deleteTaskById(Long taskId) throws Exception;
    
    public void cancelTaskById(Long taskId) throws Exception;

    public void submitJob(String processDefName, Long taskId) throws Exception;
    
    public List<Task> getUserTasks() throws Exception;

    public List<Task> getUserParentTasks() throws Exception;
    
    public List<Task> getUserTasksByType(String taskName) throws Exception;

    public Subject getSubject() throws Exception;
    
    public Subject getSubject(String nameOrKey) throws Exception;
    
    public List<Subject> getSubjects() throws Exception;

    public Subject saveOrUpdateSubject(Subject subject) throws Exception;

    public void removePreferenceCategory(String preferenceCategory) throws Exception;

	public void stopContinuousExecution(Long taskId) throws Exception;

    public Subject loginSubject() throws Exception;
    
    public void beginSession();
    
    public void endSession();
}
