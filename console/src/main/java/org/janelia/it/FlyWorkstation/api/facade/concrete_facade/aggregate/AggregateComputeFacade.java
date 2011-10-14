package org.janelia.it.FlyWorkstation.api.facade.concrete_facade.aggregate;

import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.ComputeFacade;
import org.janelia.it.FlyWorkstation.api.stub.data.DuplicateDataException;
import org.janelia.it.FlyWorkstation.api.stub.data.NoDataException;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/22/11
 * Time: 4:50 PM
 */
public class AggregateComputeFacade extends AggregateFacadeBase implements ComputeFacade {

    static private Object[] parameters = new Object[]{EntityConstants.TYPE_FOLDER};

    protected String getMethodNameForAggregates() {
        return ("getFacade");
    }

    protected Class[] getParameterTypesForAggregates() {
        return new Class[]{String.class};
    }

    protected Object[] getParametersForAggregates() {
        return parameters;
    }

    @Override
    public Task saveOrUpdateTask(Task task) throws Exception {
        Object[] aggregates = getAggregates();
        List<Task> returnList = new ArrayList<Task>();
        Task tmpTask;
        for (Object aggregate : aggregates) {
            tmpTask = ((ComputeFacade) aggregate).saveOrUpdateTask(task);
            if (tmpTask != null) {
                returnList.add(tmpTask);
            }
        }
        // Only one facade should be allowing saves of data; therefore, only one item should be returned
        if (1 < returnList.size()) {
            throw new DuplicateDataException();
        }
        if (1 == returnList.size()) {
            return returnList.get(0);
        }
        throw new NoDataException();
    }

    @Override
    public Task getTaskById(Long taskId) throws Exception {
		throw new UnsupportedOperationException();
    }

    @Override
    public void cancelTaskById(Long taskId) throws Exception {
		throw new UnsupportedOperationException();
    }
    
    @Override
    public void deleteTaskById(Long taskId) throws Exception {
        Object[] aggregates = getAggregates();
        for (Object aggregate : aggregates) {
            ((ComputeFacade) aggregate).deleteTaskById(taskId);
        }
    }
    
    @Override
	public void submitJob(String processDefName, Long taskId) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
    public List<Task> getUserParentTasks() throws Exception {
		throw new UnsupportedOperationException();
    }
    
    @Override
    public List<Task> getUserTasks() throws Exception {
		throw new UnsupportedOperationException();
    }
    
    @Override
    public List<Task> getUserTasksByType(String taskName) throws Exception {
        Object[] aggregates = getAggregates();
        List<Task> returnList = new ArrayList<Task>();
        List<Task> tmpTasks;
        for (Object aggregate : aggregates) {
            tmpTasks = ((ComputeFacade) aggregate).getUserTasksByType(taskName);
            if (tmpTasks != null) {
                returnList.addAll(tmpTasks);
            }
        }
        return returnList;
    }

    @Override
    public User getUser() throws Exception {
        Object[] aggregates = getAggregates();
        List<User> returnList = new ArrayList<User>();
        User tmpUser;
        for (Object aggregate : aggregates) {
            tmpUser = ((ComputeFacade) aggregate).getUser();
            if (tmpUser != null) {
                returnList.add(tmpUser);
            }
        }
        // Only one facade should be returning user data; therefore, only one item should be returned
        if (1 < returnList.size()) {
            throw new DuplicateDataException();
        }
        if (1 == returnList.size()) {
            return returnList.get(0);
        }
        throw new NoDataException();
    }

    @Override
    public User saveOrUpdateUser(User user) throws Exception {
        Object[] aggregates = getAggregates();
        List<User> returnList = new ArrayList<User>();
        User tmpUser;
        for (Object aggregate : aggregates) {
            tmpUser = ((ComputeFacade) aggregate).saveOrUpdateUser(user);
            if (tmpUser != null) {
                returnList.add(tmpUser);
            }
        }
        // Only one facade should be allowing saves of data; therefore, only one item should be returned
        if (1 < returnList.size()) {
            throw new DuplicateDataException();
        }
        if (1 == returnList.size()) {
            return returnList.get(0);
        }
        throw new NoDataException();
    }

    @Override
    public void removePreferenceCategory(String preferenceCategory) throws Exception {
        Object[] aggregates = getAggregates();
        for (Object aggregate : aggregates) {
            ((ComputeFacade) aggregate).removePreferenceCategory(preferenceCategory);
        }
    }
}
