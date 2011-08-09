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
        return EJBFactory.getRemoteComputeBean().saveOrUpdateTask(task);
    }

    @Override
    public void deleteTaskById(Long taskId) throws Exception {
        EJBFactory.getRemoteComputeBean().deleteTaskById(taskId);
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
    public User saveOrUpdateUser(User user) throws Exception {
        return EJBFactory.getRemoteComputeBean().saveOrUpdateUser(user);
    }

    @Override
    public void removePreferenceCategory(String preferenceCategory) throws Exception {
        EJBFactory.getRemoteComputeBean().removePreferenceCategory(preferenceCategory);
    }
}
