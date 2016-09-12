
package src.org.janelia.it.jacs.compute.service.search;

import org.hibernate.SQLQuery;
import org.hibernate.Session;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Oct 30, 2007
 * Time: 3:30:30 PM
 */
class ClusterAccessionSearchResultBuilder extends AccessionSearchResultBuilder {

    ClusterAccessionSearchResultBuilder() {
    }

    List<AccessionSearchResult> retrieveAccessionSearchResult(String acc,
                                                              Long searchResultNodeId,
                                                              Session session)
            throws Exception {
        List<AccessionSearchResult> results = null;
        AccessionSearchResultBuilder accSearchBuilder;
        String upperAcc = acc.toUpperCase();
        if (upperAcc.startsWith("CAM_CRCL_")) {
            accSearchBuilder = new CoreClusterAccessionSearchResultBuilder();
            results = accSearchBuilder.retrieveAccessionSearchResult(acc, searchResultNodeId, session);
        }
        else if (upperAcc.startsWith("CAM_CL_")) {
            accSearchBuilder = new FinalClusterAccessionSearchResultBuilder();
            results = accSearchBuilder.retrieveAccessionSearchResult(acc, searchResultNodeId, session);
        }
        else {
            _logger.info("Unrecognized cluster accession: " + "'" + acc + "'");
        }
        return results;
    }

    protected SQLQuery createSearchQuery(Object[] accessionQueryParams, Session session) {
        throw new UnsupportedOperationException();
    }
}
