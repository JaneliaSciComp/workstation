package org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb;

import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.ComputeFacade;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.User;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 8/8/11
 * Time: 11:06 AM
 */
public class EJBComputeFacade implements ComputeFacade {

    @Override
    public Task saveOrUpdateTask(Task task) throws Exception {
    	if (task == null) throw new IllegalArgumentException("Task may not be null");
        return EJBFactory.getRemoteComputeBean().saveOrUpdateTask(task);
    }

    @Override
    public void stopContinuousExecution(Long taskId) throws Exception {
    	if (taskId == null) throw new IllegalArgumentException("Task id may not be null");
        EJBFactory.getRemoteComputeBean().stopContinuousExecution(taskId);
    }

    @Override
    public Task getTaskById(Long taskId) throws Exception {
    	if (taskId == null) throw new IllegalArgumentException("Task id may not be null");
        return EJBFactory.getRemoteComputeBean().getTaskById(taskId);
    }

    @Override
    public void cancelTaskById(Long taskId) throws Exception {
    	if (taskId == null) throw new IllegalArgumentException("Task id may not be null");
        EJBFactory.getRemoteComputeBean().cancelTaskById(taskId);
    }

    @Override
    public void deleteTaskById(Long taskId) throws Exception {
    	if (taskId == null) throw new IllegalArgumentException("Task id may not be null");
        EJBFactory.getRemoteComputeBean().deleteTaskById(taskId);
    }

    @Override
    public void submitJob(String processDefName, Long taskId) throws Exception {
    	if (taskId == null) throw new IllegalArgumentException("Task id may not be null");
        EJBFactory.getRemoteComputeBean(true).submitJob(processDefName, taskId);
    }
    
    @Override
    public List<Task> getUserTasks() throws Exception {
        return EJBFactory.getRemoteComputeBean().getUserTasks(SessionMgr.getUsername());
    }

    @Override
    public List<Task> getUserParentTasks() throws Exception {
        return EJBFactory.getRemoteComputeBean().getUserParentTasks(SessionMgr.getUsername());
    }
    
    @Override
    public List<Task> getUserTasksByType(String taskName) throws Exception {
        return EJBFactory.getRemoteComputeBean().getUserTasksByType(taskName, SessionMgr.getUsername());
    }

    @Override
    public User getUser() throws Exception {
        return EJBFactory.getRemoteComputeBean().getUserByName(SessionMgr.getUsername());
    }

    @Override
    public List getUsers() throws Exception {
        return EJBFactory.getRemoteComputeBean().getUsers();
    }

    @Override
    public User saveOrUpdateUser(User user) throws Exception {
        return EJBFactory.getRemoteComputeBean().saveOrUpdateUser(user);
    }

    @Override
    public boolean loginUser() throws Exception {
        return EJBFactory.getRemoteComputeBean().login(
        		(String)SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_NAME),
                (String)SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_PASSWORD));
    }

    @Override
    public void removePreferenceCategory(String preferenceCategory) throws Exception {
        EJBFactory.getRemoteComputeBean().removePreferenceCategory(preferenceCategory);
    }
}
