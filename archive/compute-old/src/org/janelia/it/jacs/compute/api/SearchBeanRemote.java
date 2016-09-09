
package src.org.janelia.it.jacs.compute.api;

import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.search.SearchResultNode;

import javax.ejb.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Oct 26, 2007
 * Time: 2:07:36 PM
 */
@Remote
public interface SearchBeanRemote {

    public void submitSearchTask(long searchTaskId) throws RemoteException;

    public Task saveOrUpdateTask(Task task) throws DaoException, RemoteException;

    public Task getTaskById(long taskId) throws RemoteException;

    public SearchResultNode getSearchTaskResultNode(long searchTaskId) throws DaoException, RemoteException;

    public Task getTaskWithEventsById(long taskId) throws DaoException, RemoteException;

    public Task getTaskWithResultsById(long taskId) throws DaoException, RemoteException;

    //    public Event saveEvent(Task task, String eventType, String description, Date timestamp) throws DaoException, RemoteException;
    public int populateSearchResult(Long searchTaskId, List<String> topic) throws DaoException, RemoteException;

    public void search(String searchType, String searchString) throws RemoteException;
}
