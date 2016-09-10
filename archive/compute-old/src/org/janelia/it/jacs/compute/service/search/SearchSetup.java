
package src.org.janelia.it.jacs.compute.service.search;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.TimebasedIdentifierGenerator;
import org.janelia.it.jacs.model.tasks.search.SearchTask;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Oct 29, 2007
 * Time: 4:07:53 PM
 */

public class SearchSetup implements IService {
    SearchTask _searchTask;
    ComputeDAO _computeDAO;

    private Logger _logger;
    private static Map<Long, Object> synchMap = new HashMap<Long, Object>();
    private static long MILISECS_TO_KEEP_ID = 10 * 60 * 1000L; // 10 minutes
    private static long CLEANING_INTERVAL = 5 * 60 * 1000L; // 5 minutes
    private static Date cleanAtTimer = new Date();

    public SearchSetup() {
    }

    public void execute(IProcessData processData) throws SystemSearchException {
        try {
            _logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            _computeDAO = new ComputeDAO(_logger);
            cleanObjectMap(_logger);
            init(processData);
            List<String> topics = _searchTask.getSearchTopics();
            List<String> permissibleFastTopics = new ArrayList<String>();
            List<String> permissibleSlowTopics = new ArrayList<String>();
            List<String> permissibleGridTopics = new ArrayList<String>();
            for (String topic : topics) {
                if (topic.equals(SearchTask.TOPIC_CLUSTER) ||
                        topic.equals(SearchTask.TOPIC_PROTEIN)) {
                    permissibleGridTopics.add(topic);
                }
                else if (topic.equals(SearchTask.TOPIC_SAMPLE) ||
                        topic.equals(SearchTask.TOPIC_PUBLICATION) ||
                        topic.equals(SearchTask.TOPIC_PROJECT)) {
                    permissibleFastTopics.add(topic);
                }
                else if (topic.equals(SearchTask.TOPIC_WEBSITE) ||
                        topic.equals(SearchTask.TOPIC_ACCESSION)) {
                    permissibleSlowTopics.add(topic);
                }
                else {
                    _logger.info("Search skipping searchId=" + processData.getProcessId() + " topic=" + topic);
                }
            }
            processData.putItem("SEARCH_FAST_CATEGORIES", permissibleFastTopics);
            processData.putItem("SEARCH_SLOW_CATEGORIES", permissibleSlowTopics);
            processData.putItem("SEARCH_GRID_CATEGORIES", permissibleGridTopics);
        }
        catch (Exception e) {
            throw new SystemSearchException(e);
        }
    }

    protected void init(IProcessData processData) throws MissingDataException {
        try {
            _searchTask = (SearchTask) _computeDAO.getTaskById(processData.getProcessId());
            if (_searchTask == null)
                throw new Exception("searchTask is null for id=" + processData.getProcessId());
            _logger.debug("Adding synch object for task " + _searchTask.getObjectId() + "(hash:" + System.identityHashCode(_searchTask.getObjectId()) + ")");
            synchMap.put(_searchTask.getObjectId(), new Object());
        }
        catch (Exception e) {
            throw new MissingDataException("Could not load SearchTask for id=" + processData.getProcessId() + " :" + e.getMessage());
        }
    }

    public static Object getSynchObjectForTask(Long taskID) {
        //        _logger.debug("For task " + taskID + "(hash:" +System.identityHashCode(taskID) + ") synch object hash = " + System.identityHashCode(o));
        return synchMap.get(taskID);
    }

    private static synchronized void cleanObjectMap(Logger logger) {
        Date now = new Date();
        if (cleanAtTimer.compareTo(now) <= 0) // ready for cleaning!
        {
            logger.debug("Commencing synchMap cleaning");
            cleanAtTimer.setTime(now.getTime() + CLEANING_INTERVAL); // reset timer
            Date idDate;
            int count = 0;
            // must get a copy to avoid ConcurrentModificationException
            Long[] allIDs = setToLongArray(synchMap.keySet());
            for (Long id : allIDs) {
                idDate = TimebasedIdentifierGenerator.getTimestamp(id);
                if (now.getTime() - idDate.getTime() > MILISECS_TO_KEEP_ID) {
                    count++;
                    synchMap.remove(id);
                }
            }
            logger.debug("Finished synchMap cleaning. Removed " + count + " objects");
        }
    }

    private static Long[] setToLongArray(Set<Long> items) {
        Long[] arr = new Long[items.size()];
        Iterator iter = items.iterator();
        for (int i = 0; i < items.size(); i++)
            arr[i] = (Long) iter.next();

        return arr;
    }
}

