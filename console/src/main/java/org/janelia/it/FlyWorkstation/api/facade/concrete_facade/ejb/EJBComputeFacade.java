package org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb;

import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.ComputeFacade;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.ConsoleProperties;
import org.janelia.it.FlyWorkstation.shared.util.filecache.WebDavClient;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.user_data.User;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
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
        return EJBFactory.getRemoteComputeBean().getUserTasks(SessionMgr.getSubjectKey());
    }

    @Override
    public List<Task> getUserParentTasks() throws Exception {
        return EJBFactory.getRemoteComputeBean().getRecentUserParentTasks(SessionMgr.getSubjectKey());
    }
    
    @Override
    public List<Task> getUserTasksByType(String taskName) throws Exception {
        return EJBFactory.getRemoteComputeBean().getUserTasksByType(taskName, SessionMgr.getSubjectKey());
    }

    @Override
    public User getUser() throws Exception {
        return EJBFactory.getRemoteComputeBean().getUserByNameOrKey(SessionMgr.getSubjectKey());
    }

    @Override
    public List<Subject> getSubjects() throws Exception {
        return EJBFactory.getRemoteComputeBean().getSubjects();
    }

    @Override
    public User saveOrUpdateUser(User user) throws Exception {
        return EJBFactory.getRemoteComputeBean().saveOrUpdateUser(user);
    }

    @Override
    public User loginUser() throws Exception {
        final SessionMgr mgr = SessionMgr.getSessionMgr();
        final String userName = (String)
                mgr.getModelProperty(SessionMgr.USER_NAME);
        final String password = (String)
                mgr.getModelProperty(SessionMgr.USER_PASSWORD);
        final ComputeBeanRemote compute = EJBFactory.getRemoteComputeBean();
        final User loggedInUser = compute.login(userName, password);

        // set default authenticator for all http requests
        if (null!=loggedInUser) {
            Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(userName,
                                                      password.toCharArray());
                }
            });
            WebDavClient webDavClient = mgr.getWebDavClient();
            webDavClient.setCredentialsUsingAuthenticator();
        }

        return loggedInUser;
    }
    
    @Override
    public void beginSession() {
        EJBFactory.getRemoteComputeBean().beginSession(
        		(String)SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_NAME),
                ConsoleProperties.getString("console.versionNumber"));
    }
    
    @Override
    public void endSession(String username) {
    	EJBFactory.getRemoteComputeBean().endSession(username);
    }
    
    @Override
    public void removePreferenceCategory(String preferenceCategory) throws Exception {
        EJBFactory.getRemoteComputeBean().removePreferenceCategory(preferenceCategory);
    }
}
