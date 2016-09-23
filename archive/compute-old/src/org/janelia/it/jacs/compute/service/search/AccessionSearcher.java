
package src.org.janelia.it.jacs.compute.service.search;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.janelia.it.jacs.model.tasks.search.SearchTask;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Oct 30, 2007
 * Time: 3:30:30 PM
 */

public class AccessionSearcher {
    Logger _logger = Logger.getLogger(this.getClass());

    private Session _session;

    public AccessionSearcher(Session session) {
        _session = session;
    }

    public int populateSearchResult(SearchTask searchTask)
            throws Exception {
        _logger.debug("Searching category '" + SearchTask.TOPIC_ACCESSION + "'");
        String searchQuery = searchTask.getSearchString();
        if (searchQuery == null || searchQuery.trim().length() == 0) {
            throw new IllegalArgumentException("Invalid accession");
        }
        String searchedAccession = searchQuery.trim();
        int cnt = populateSearchResult(searchedAccession, searchTask.getSearchResultNode().getObjectId());
        _logger.debug(SearchTask.TOPIC_ACCESSION + " search complete. Found " + cnt + " hits");
        return cnt;
    }

    private int populateSearchResult(String searchedAccession, Long searchResultNodeId)
            throws Exception {
        final int NO_RESULTS = 0;
        int nResults;
        // we could implement a chain of responsibility pattern here
        // but for now I'll stick with one big ugly series of ifs
        AccessionSearchResultBuilder accSearchResultBuilder;
        accSearchResultBuilder = new JacsAccessionSearchResultBuilder();
        // try an accession search
        nResults = accSearchResultBuilder.populateAccessionSearchResult(searchedAccession,
                searchResultNodeId, _session);
        if (nResults > 0) {
            return nResults;
        }
        // try a GI number match
        accSearchResultBuilder = new NCBIGIAccessionSearchResultBuilder();
        nResults = accSearchResultBuilder.populateAccessionSearchResult(searchedAccession,
                searchResultNodeId, _session);
        if (nResults > 0) {
            return nResults;
        }
        // try a sample name search
        accSearchResultBuilder = new SampleNameSearchResultBuilder();
        nResults = accSearchResultBuilder.populateAccessionSearchResult(searchedAccession,
                searchResultNodeId, _session);
        if (nResults > 0) {
            return nResults;
        }
        // try an exact external accession match
        accSearchResultBuilder = new ExactExternalAccessionSearchResultBuilder();
        nResults = accSearchResultBuilder.populateAccessionSearchResult(searchedAccession,
                searchResultNodeId, _session);
        if (nResults > 0) {
            return nResults;
        }
        // try a versioned external accession match
        accSearchResultBuilder = new VersionedExternalAccessionSearchResultBuilder();
        nResults = accSearchResultBuilder.populateAccessionSearchResult(searchedAccession,
                searchResultNodeId, _session);
        if (nResults > 0) {
            return nResults;
        }
        return NO_RESULTS;
    }

}
