package org.janelia.it.workstation.api.facade.concrete_facade.ejb;

import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.user_data.UserToolEvent;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 8/8/11
 * Time: 11:06 AM
 */
public class EJBComputeFacade implements org.janelia.it.workstation.api.facade.abstract_facade.ComputeFacade {

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
        return EJBFactory.getRemoteComputeBean().getUserTasks(org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSubjectKey());
    }

    @Override
    public List<Task> getUserParentTasks() throws Exception {
        return EJBFactory.getRemoteComputeBean().getRecentUserParentTasks(org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSubjectKey());
    }
    
    @Override
    public List<Task> getUserTasksByType(String taskName) throws Exception {
        return EJBFactory.getRemoteComputeBean().getUserTasksByType(taskName, org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSubjectKey());
    }

    @Override
    public Subject getSubject() throws Exception {
        return EJBFactory.getRemoteComputeBean().getSubjectWithPreferences(org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSubjectKey());
    }

    @Override
    public Subject getSubject(String nameOrKey) throws Exception {
        return EJBFactory.getRemoteComputeBean().getSubjectWithPreferences(nameOrKey);
    }
    
    @Override
    public List<Subject> getSubjects() throws Exception {
        return EJBFactory.getRemoteComputeBean().getSubjects();
    }

    @Override
    public Subject saveOrUpdateSubject(Subject subject) throws Exception {
        return EJBFactory.getRemoteComputeBean().saveOrUpdateSubject(subject);
    }

    @Override
    public Subject loginSubject() throws Exception {
        final org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr mgr = org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr();
        final String userName = (String) mgr.getModelProperty(org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.USER_NAME);
        final String password = (String) mgr.getModelProperty(org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.USER_PASSWORD);
        final ComputeBeanRemote compute = EJBFactory.getRemoteComputeBean();
        final Subject loggedInSubject = compute.login(userName, password);

        // set default authenticator for all http requests
        if (null!=loggedInSubject) {
            Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(userName,
                                                      password.toCharArray());
                }
            });
            org.janelia.it.workstation.shared.util.filecache.WebDavClient webDavClient = mgr.getWebDavClient();
            webDavClient.setCredentialsUsingAuthenticator();
        }

        return loggedInSubject;
    }
    
    @Override
    public void beginSession() {
        UserToolEvent loginEvent = EJBFactory.getRemoteComputeBean().beginSession(
                org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().getSubject().getName(),
                org.janelia.it.workstation.shared.util.ConsoleProperties.getString("console.Title"),
                org.janelia.it.workstation.shared.util.ConsoleProperties.getString("console.versionNumber"));
        if (null!=loginEvent && null!=loginEvent.getSessionId()) {
            org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().setCurrentSessionId(loginEvent.getSessionId());
        }
    }
    
    @Override
    public void endSession() {
    	EJBFactory.getRemoteComputeBean().endSession(
                org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().getSubject().getName(),
                org.janelia.it.workstation.shared.util.ConsoleProperties.getString("console.Title"),
                org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().getCurrentSessionId());
        org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().setCurrentSessionId(null);
    }
    
    @Override
    public void removePreferenceCategory(String preferenceCategory) throws Exception {
        EJBFactory.getRemoteComputeBean().removePreferenceCategory(preferenceCategory);
    }
}
