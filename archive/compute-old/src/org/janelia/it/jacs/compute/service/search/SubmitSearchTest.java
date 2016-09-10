
package src.org.janelia.it.jacs.compute.service.search;

import org.janelia.it.jacs.model.tasks.search.SearchTask;

import java.util.Arrays;
import java.util.List;

/**
 * The class contain only tests for search task submission without any result checking
 * User: cgoina
 * Date: Apr 19, 2007
 * Time: 2:54:23 PM
 *
 */
public class SubmitSearchTest extends SearchTestBase {

    public SubmitSearchTest() {
        super();
    }

    public SubmitSearchTest(String name) {
        super(name);
    }

    public void testSubmitSearchAll() {
        String searchString = "itshouldnotfindanything";
        submitSearchALLForSuccess(searchString);
    }

    public void testSubmitCategorySearch() {
        String searchString = "itshouldnotfindanything";
        submitCategorySearch(searchString,SearchTask.TOPIC_ACCESSION);
        submitCategorySearch(searchString,SearchTask.TOPIC_CLUSTER);
        submitCategorySearch(searchString,SearchTask.TOPIC_PROTEIN);
        submitCategorySearch(searchString,SearchTask.TOPIC_PROJECT);
        submitCategorySearch(searchString,SearchTask.TOPIC_PUBLICATION);
        submitCategorySearch(searchString,SearchTask.TOPIC_SAMPLE);
        submitCategorySearch(searchString,SearchTask.TOPIC_WEBSITE);
    }

    protected void submitSearchALLForSuccess(String searchString) {
        SearchTaskStatus taskStatus = null;
        try {
            long timeout = 600000L;
            taskStatus = submitJobAndWaitForCompletion("itshouldnotfindanything",null, SearchTask.MATCH_ALL,timeout);
        } catch(Exception e) {
            fail("Submit or execute search failed: " + e.toString());
        }
        if(taskStatus.taskCompletionStatus == SearchTask.SEARCH_TIMEDOUT) {
            fail("Search all for '" + searchString + "' timed out ");
        } else if(taskStatus.taskCompletionStatus == SearchTask.SEARCH_COMPLETED_WITH_ERRORS) {
            fail("Search all for '" + searchString + "' finished with errors ");
        }
    }

    protected void submitCategorySearch(String searchString, String searchCategory) {
        SearchTaskStatus taskStatus = null;
        try {
            List searchCategories = Arrays.asList(searchCategory);
            long timeout = 600000L;
            taskStatus = submitJobAndWaitForCompletion(searchString,searchCategories,SearchTask.MATCH_ALL,timeout);
        } catch(Exception e) {
            fail("Submit or execute search for " + searchCategory + " failed: " + e.toString());
        }
        if(taskStatus.taskCompletionStatus == SearchTask.SEARCH_TIMEDOUT) {
            fail("Search " + searchCategory + " for '" + searchString + "' timed out ");
        } else if(taskStatus.taskCompletionStatus == SearchTask.SEARCH_COMPLETED_WITH_ERRORS) {
            fail("Search " + searchCategory + " for '" + searchString + "' finished with errors ");
        }
    }

}
