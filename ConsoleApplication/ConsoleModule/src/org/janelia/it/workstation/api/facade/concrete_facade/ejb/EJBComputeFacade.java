package org.janelia.it.workstation.api.facade.concrete_facade.ejb;

import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.user_data.UserToolEvent;
import org.janelia.it.workstation.api.facade.abstract_facade.ComputeFacade;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.util.ConsoleProperties;
import org.janelia.it.workstation.shared.util.filecache.WebDavClient;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 8/8/11
 * Time: 11:06 AM
 */
public class EJBComputeFacade implements ComputeFacade {

    private static final Logger log = LoggerFactory.getLogger(EJBComputeFacade.class);
    
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
        return EJBFactory.getRemoteComputeBean().getUserTasks("SessionMgr.getSubjectKey()");
    }

    @Override
    public List<Task> getUserParentTasks() throws Exception {
        return EJBFactory.getRemoteComputeBean().getRecentUserParentTasks("SessionMgr.getSubjectKey()");
    }
    
    @Override
    public List<Task> getUserTasksByType(String taskName) throws Exception {
        return EJBFactory.getRemoteComputeBean().getUserTasksByType(taskName, "SessionMgr.getSubjectKey()");
    }

    @Override
    public Subject saveOrUpdateSubject(Subject subject) throws Exception {
        return EJBFactory.getRemoteComputeBean().saveOrUpdateSubject(subject);
    }

    @Override
    public void removePreferenceCategory(String preferenceCategory) throws Exception {
        EJBFactory.getRemoteComputeBean().removePreferenceCategory(preferenceCategory);
    }
}
