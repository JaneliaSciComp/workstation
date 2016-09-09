
package src.org.janelia.it.jacs.compute.service.search;

import junit.framework.TestCase;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.SearchBeanRemote;
import org.janelia.it.jacs.compute.api.TaskServiceProperties;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.search.SearchResultNode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tnabeel
 * Date: Apr 19, 2007
 * Time: 2:54:23 PM
 *
 */
public abstract class SearchTestBase extends TestCase {
    protected SearchBeanRemote searchBean;
    protected TaskServiceProperties searchProperties;
    protected ComputeBeanRemote computeBean;

    private static final boolean CLEAN_DATA_AFTER_RUN = SystemConfigurationProperties.getBoolean("junit.test.cleanDataAfterRun");
    private static final String TEST_USER_NAME = SystemConfigurationProperties.getString("junit.test.username");

    protected class SearchTaskStatus {
        protected SearchTask searchTask;
        protected int taskCompletionStatus;
        protected SearchTaskStatus() {
        }
        protected SearchTaskStatus(SearchTask st,int status) {
            searchTask = st;
            taskCompletionStatus = status;
        }
    }

    public SearchTestBase() {
        super();
    }

    public SearchTestBase(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        // Load blast parameter values
        searchBean = EJBFactory.getRemoteSearchBean();
        computeBean = EJBFactory.getRemoteComputeBean();
    }

    /**
     * This runs after every test and connection creations is expensive.  It makes sense though
     * considering that one could run a single test that persists lots of data.
     * @throws Exception
     */
    protected void tearDown() throws Exception {
        if (CLEAN_DATA_AFTER_RUN) {
            Class.forName(SystemConfigurationProperties.getString("jdbc.driverClassName"));
            Connection connection = DriverManager.getConnection(
                    SystemConfigurationProperties.getString("jdbc.url"),
                    SystemConfigurationProperties.getString("jdbc.username"),
                    SystemConfigurationProperties.getString("jdbc.password"));
            connection.setAutoCommit(false);
            PreparedStatement pstmt = connection.prepareStatement("select delete_user_data('"+ TEST_USER_NAME+"',false)");
            pstmt.executeQuery();
            connection.commit();
            pstmt.close();
            connection.close();
        }                                                                                  
    }

    protected SearchTaskStatus submitJobAndWaitForCompletion(String searchString,
                                                             List<String> searchTopics,
                                                             int matchFlags,
                                                             long timeout)
            throws Exception {
        // create the task
        SearchTask searchTask = (SearchTask)createSearchTask(searchString,searchTopics,matchFlags);
        // submit the task to the server
        searchBean.submitSearchTask(searchTask.getObjectId());
        // wait for the results
        int completionStatus = verifyCompletion(searchTask.getObjectId(),timeout);
        return new SearchTaskStatus(searchTask,completionStatus);
    }

    /**
     * checks whether the task has completed
     * @param taskId the task to be checked
     * @return 0 if the task has completed successfully
     *         -1 if the task has completed with errors
     *         1 if the task is still running
     *         2 if the task has timed out
     * @throws Exception
     */
    protected int verifyCompletion(Long taskId,long timeout) throws Exception {
        long startTime = System.currentTimeMillis();
        int completionStatus = 1;
        while(true) {
            SearchTask t = (SearchTask)searchBean.getTaskWithEventsById(taskId);
            completionStatus = t.hasCompleted();
            if(completionStatus == SearchTask.SEARCH_STILL_RUNNING) {
                Thread.sleep(5000);
                if(System.currentTimeMillis() - startTime > timeout) {
                    return SearchTask.SEARCH_TIMEDOUT; // timed out
                }
            } else {
                break;
            }
        }
        return completionStatus;
    }

    private Task createSearchTask(String searchString,
                                  List<String> searchTopics,
                                  int matchFlags)
            throws Exception {
        User testUser = getTestUser();
        // create the task entry
        SearchTask searchTask = new SearchTask();
        searchTask.setSearchString(searchString);
        if(searchTopics == null) {
            searchTopics = searchTask.getAllSupportedTopics();
        }
        searchTask.setSearchTopics(searchTopics);
        searchTask.setMatchFlags(matchFlags);
        searchTask.setOwner(testUser.getName());
        for(String topic: searchTopics) {
            if(!searchTask.isTopicSupported(topic)) {
                throw new IllegalArgumentException("Invalid topic: " + topic);
            }
            Event searchTopicEvent = new Event(topic,new Date(),Event.SUBTASKRUNNING_EVENT);
            searchTask.addEvent(searchTopicEvent);
        }
        // create the result node
        SearchResultNode searchResultNode = new SearchResultNode();
        searchResultNode.setTask(searchTask);
        searchResultNode.setOwner(testUser.getName());
        searchTask.addOutputNode(searchResultNode);
        return searchBean.saveOrUpdateTask(searchTask);
    }

    private User getTestUser() throws Exception {
        return computeBean.getUserByNameOrKey(TEST_USER_NAME);
    }

}
