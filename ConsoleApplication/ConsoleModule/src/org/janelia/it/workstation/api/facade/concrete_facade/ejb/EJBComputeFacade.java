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
    public Subject getSubject() throws Exception {
        return EJBFactory.getRemoteComputeBean().getSubjectWithPreferences(SessionMgr.getSubjectKey());
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
        final SessionMgr mgr = SessionMgr.getSessionMgr();
        final String userName = (String) mgr.getModelProperty(SessionMgr.USER_NAME);
        final String password = (String) mgr.getModelProperty(SessionMgr.USER_PASSWORD);
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
            WebDavClient webDavClient = mgr.getWebDavClient();
            webDavClient.setCredentialsUsingAuthenticator();
        }

        return loggedInSubject;
    }
    
    @Override
    public void beginSession() {
        UserToolEvent loginEvent = EJBFactory.getRemoteComputeBean().beginSession(
                SessionMgr.getSessionMgr().getSubject().getName(),
                ConsoleProperties.getString("console.Title"),
                ConsoleProperties.getString("console.versionNumber"));
        if (null!=loginEvent && null!=loginEvent.getSessionId()) {
            SessionMgr.getSessionMgr().setCurrentSessionId(loginEvent.getSessionId());
        }
    }
    
    @Override
    public void endSession() {
        try{
            EJBFactory.getRemoteComputeBean().endSession(
                SessionMgr.getSessionMgr().getSubject().getName(),
                ConsoleProperties.getString("console.Title"),
                SessionMgr.getSessionMgr().getCurrentSessionId());
            SessionMgr.getSessionMgr().setCurrentSessionId(null);
        } catch( NullPointerException npe){   
            // This is for bug FW-2512.  At time of writing, unable to duplicate
            // this exception. This bug may have been fixed by unrelated commit.
            // Therefore, leaving this trail to see which
            // possible thing is null. LLF, 6/1/2014.
            System.out.println("Remote compute bean: " + EJBFactory.getRemoteComputeBean());
            System.out.println("SessionMgr: " + SessionMgr.getSessionMgr());
            System.out.println("Subject: " + SessionMgr.getSessionMgr().getSubject());
            throw npe;
        }
    }
    
    @Override
    public void removePreferenceCategory(String preferenceCategory) throws Exception {
        EJBFactory.getRemoteComputeBean().removePreferenceCategory(preferenceCategory);
    }
}
