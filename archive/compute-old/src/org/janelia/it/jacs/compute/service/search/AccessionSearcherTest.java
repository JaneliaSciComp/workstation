
package src.org.janelia.it.jacs.compute.service.search;

import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.model.user_data.search.SearchResultNode;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 12, 2007
 * Time: 1:01:36 PM
 *
 */
public class AccessionSearcherTest extends AbstractBaseAccessionSearchTest {

    public AccessionSearcherTest(String name) {
        super(name);
    }

    public void testExistingAccessionSearchResult() {
        testExistingAccessionSearchResult("jcvi_read_299866");
        testExistingAccessionSearchResult("JCVI_TGI_1096124280446");
        testExistingAccessionSearchResult("cam_crcl_1");
        testExistingAccessionSearchResult("cam_proj_gos");
        testExistingAccessionSearchResult("jcvi_smpl_1103283000030");
        testExistingAccessionSearchResult("gs000a");
        testExistingAccessionSearchResult("gi|63489920");
        testExistingAccessionSearchResult("63489920");
        testExistingAccessionSearchResult("aagw01523655.1");
        testExistingAccessionSearchResult("aagw01523655");
    }

    public void testValidNonExistingAccessionSearchResult() {
        testValidNonExistingAccessionSearchResult("jcvi_read_-1");
    }

    private void testExistingAccessionSearchResult(String testAcc) {
        try {
            int nResults = testAccessionSearchResult(testAcc);
            assertTrue(nResults == 1);
        } catch(Exception e) {
            fail("testExistingAccessionSearchResult " + testAcc);
        }
    }

    private void testValidNonExistingAccessionSearchResult(String testAcc) {
        try {
            int nResults = testAccessionSearchResult(testAcc);
            assertTrue(nResults == 0);
        } catch(Exception e) {
            fail("testValidNonExistingAccessionSearchResult " + testAcc);
        }
    }

    protected int testAccessionSearchResult(String acc)
            throws Exception {
        AccessionSearcher accSearcher = new AccessionSearcher(getCurrentSession());
        ArrayList accessionTopic = new ArrayList();
        accessionTopic.add(SearchTask.TOPIC_ACCESSION);
        SearchResultNode srn = new SearchResultNode();
        srn.setObjectId(new Long(-1));
        HashSet searchResults = new HashSet();
        searchResults.add(srn);
        SearchTask sTask = new SearchTask();
        sTask.setSearchString(acc);
        sTask.setSearchTopics(accessionTopic);
        sTask.setOutputNodes(searchResults);
        int nResults = accSearcher.populateSearchResult(sTask);
        return nResults;
    }

}
