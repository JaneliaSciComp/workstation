
package src.org.janelia.it.jacs.compute.service.search;

import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.hibernate.Session;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Oct 30, 2007
 * Time: 3:30:30 PM
 */
abstract class AccessionSearchResultBuilder {
    static private long _nextDocumentId = 0;

    Logger _logger = Logger.getLogger(this.getClass());

    protected static class AccessionSearchResult {
        Long searchResultNodeId; // search result node key
        Long accessionDocumentId; // indexed document ID 
        String accession; // found accession
        String documentName; // document's specific name
        String accessionType; // type of accession
        String searchedAccession; // accesion used for querying

        AccessionSearchResult() {
        }

        public String toString() {
            StringBuffer accResultBuffer = new StringBuffer();
            accResultBuffer.append('(');
            accResultBuffer.append("nodeId=").append(searchResultNodeId);
            accResultBuffer.append('\'');
            accResultBuffer.append("docId=").append(accessionDocumentId);
            accResultBuffer.append('\'');
            accResultBuffer.append("accession=").append(accession);
            accResultBuffer.append('\'');
            accResultBuffer.append("accessionType=").append(accessionType);
            accResultBuffer.append('\'');
            accResultBuffer.append("searchedAccession=").append(searchedAccession);
            accResultBuffer.append(')');
            return accResultBuffer.toString();
        }

    }

    AccessionSearchResultBuilder() {
    }

    int populateAccessionSearchResult(String acc,
                                      Long searchResultNodeId,
                                      Session session)
            throws Exception {
        List<AccessionSearchResult> accessionResults = retrieveAccessionSearchResult(acc,
                searchResultNodeId,
                session);
        return insertSearchResults(accessionResults, session);
    }

    List<AccessionSearchResult> retrieveAccessionSearchResult(String acc,
                                                              Long searchResultNodeId,
                                                              Session session)
            throws Exception {
        SQLQuery sqlQuery = createSearchQuery(new Object[]{acc}, session);
        return querySearchResult(acc, searchResultNodeId, sqlQuery);
    }

    protected abstract SQLQuery createSearchQuery(Object[] accessionQueryParams, Session session);

    protected AccessionSearchResult extractAccessionResult(String searchedAccession,
                                                           Long searchResultNodeId,
                                                           Object[] queryResult) {
        AccessionSearchResult accResult = new AccessionSearchResult();
        accResult.searchResultNodeId = searchResultNodeId;
        accResult.accessionDocumentId = getNextAccessionDocId();
        accResult.accession = (String) queryResult[0];
        accResult.accessionType = (String) queryResult[1];
        accResult.documentName = (String) queryResult[2];
        accResult.searchedAccession = searchedAccession;
        return accResult;
    }

    protected List<AccessionSearchResult> querySearchResult(String searchedAccession,
                                                            Long searchResultNodeId,
                                                            SQLQuery sqlQuery)
            throws Exception {
        // invoke the query
        List<Object[]> queryResults = sqlQuery.list();
        List<AccessionSearchResult> accessionResults = new ArrayList<AccessionSearchResult>();
        for (Object[] result : queryResults) {
            accessionResults.add(extractAccessionResult(searchedAccession,
                    searchResultNodeId,
                    result));
        }
        return accessionResults;
    }

    protected int insertSearchResults(List<AccessionSearchResult> results,
                                      Session session)
            throws Exception {
        if (results == null) {
            return -1;
        }
        else if (results.size() == 0) {
            return 0;
        }
        int nResults;
        String sql = "insert into accession_ts_result " +
                "(" +
                "node_id," +
                "docid," +
                "accession," +
                "docname," +
                "doctype," +
                "headline" +
                ") " +
                "values " +
                "(" +
                ":searchResultNodeId," +
                ":accessionDocumentId," +
                ":accession," +
                ":documentName," +
                ":accessionType," +
                ":searchedAccession" +
                ")";
        SQLQuery sqlQuery = session.createSQLQuery(sql);
        _logger.debug("Insert sql: " + sql);
        AccessionSearchResult accResult = results.get(0);
        try {
            sqlQuery.setLong("searchResultNodeId", accResult.searchResultNodeId);
            sqlQuery.setLong("accessionDocumentId", accResult.accessionDocumentId);
            sqlQuery.setString("accession", accResult.accession);
            sqlQuery.setString("documentName", accResult.documentName);
            sqlQuery.setString("accessionType", accResult.accessionType);
            sqlQuery.setString("searchedAccession", accResult.searchedAccession);
            // invoke the query
            nResults = sqlQuery.executeUpdate();
        }
        catch (Exception e) {
            _logger.error("Error executing the query: " + sql + " for " +
                    accResult);
            throw e;
        }
        return nResults;
    }

    synchronized private Long getNextAccessionDocId() {
        return ++_nextDocumentId;
    }

}
