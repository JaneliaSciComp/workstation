
package src.org.janelia.it.jacs.compute.api;

import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.search.SearchResultNode;

import javax.ejb.Local;
import java.util.List;


/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Oct 26, 2007
 * Time: 2:07:20 PM
 */
@Local
public interface SearchBeanLocal {

    public void submitSearchTask(long searchTaskId) throws Exception;

    public Task saveOrUpdateTask(Task task) throws DaoException;

    public Task getTaskById(long taskId) throws DaoException;

    public SearchResultNode getSearchTaskResultNode(long searchTaskId) throws DaoException;

    public Task getTaskWithEventsById(long taskId) throws DaoException;

    public Task getTaskWithResultsById(long taskId) throws DaoException;

    //    public Event saveEvent(Task task, String eventType, String description, Date timestamp) throws DaoException;
    public int populateSearchResult(Long searchTaskId, List<String> topic) throws DaoException;

}
