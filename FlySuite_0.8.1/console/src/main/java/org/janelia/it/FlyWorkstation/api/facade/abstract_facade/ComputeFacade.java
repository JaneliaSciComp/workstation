package org.janelia.it.FlyWorkstation.api.facade.abstract_facade;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.User;

import java.util.List;

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

    public User getUser() throws Exception;

    public List getUsers() throws Exception;

    public User saveOrUpdateUser(User user) throws Exception;

    public void removePreferenceCategory(String preferenceCategory) throws Exception;

	public void stopContinuousExecution(Long taskId) throws Exception;

    public boolean loginUser() throws Exception;
}
