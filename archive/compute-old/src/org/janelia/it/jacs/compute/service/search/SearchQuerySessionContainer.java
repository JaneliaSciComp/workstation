
package src.org.janelia.it.jacs.compute.service.search;

import org.apache.log4j.Logger;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.hibernate.SQLQuery;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.model.user_data.search.SearchResultNode;
import src.org.janelia.it.jacs.shared.lucene.LuceneDataFactory;
import src.org.janelia.it.jacs.shared.lucene.searchers.LuceneSearcher;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Oct 30, 2007
 * Time: 3:30:30 PM
 */

public class SearchQuerySessionContainer {
    Logger _logger = Logger.getLogger(this.getClass());
    ComputeBeanLocal computeBean = EJBFactory.getLocalComputeBean();
    String searchEngine;
    private ComputeDAO _computeDAO;
    private Connection suConnection;
    //    private final static int MAX_INSERT_BATCH_SIZE = SystemConfigurationProperties.getInt("ts.maxInsertBatchSize");
    private static final int MAX_SEARCH_RESULT_SET = SystemConfigurationProperties.getInt("ts.maxSearchResultSet");
    public static final String SEARCH_ENGINE_TSEARCH2 = "tsearch2";
    public static final String SEARCH_ENGINE_LUCENE = "lucene";


    public SearchQuerySessionContainer(String searchEngine, ComputeDAO computeDAO) {
        _computeDAO = computeDAO;
        this.searchEngine = searchEngine;
    }

    private void initSuConnection() throws SQLException, ClassNotFoundException {
        if (suConnection == null) {
            Class.forName(SystemConfigurationProperties.getString("jdbc.driverClassName"));
            suConnection = DriverManager.getConnection(
                    SystemConfigurationProperties.getString("jdbc.url"),
                    SystemConfigurationProperties.getString("ts.jdbc.username"),
                    SystemConfigurationProperties.getString("ts.jdbc.password"));
            suConnection.setAutoCommit(false);
        }
    }

    private void closeSuConnection() {
        if (suConnection != null) {
            try {
                suConnection.close();
            }
            catch (Exception e) {
                _logger.error("Unable to close su connection to the DB", e);
            }
            suConnection = null;
        }
    }


    public static String getSearchQuery(String category) throws Exception {
        return category + "_ftsearch(:searchResultNode,:searchString,:matchFlags)";
    }

    public static void setCategorySearchQueryParameters(SQLQuery sqlQuery,
                                                        Long searchResultNode,
                                                        String searchString,
                                                        String matchFlags) {
        sqlQuery.setLong("searchResultNode", searchResultNode);
        sqlQuery.setString("searchString", searchString);
        sqlQuery.setString("matchFlags", matchFlags);
    }

    public int populateSearchResult(SearchTask searchTask, List<String> searchCategories)
            throws Exception {
        // regular text search
        int total = 0;
        String tmpSearchCategory = "";

        try {
            initSuConnection();
            total = 0;
            for (String searchCategory : searchCategories) {
                addSubprocessEvent(Event.SUBTASKRUNNING_EVENT, searchTask, searchCategory);
                if (searchCategory.equals(SearchTask.TOPIC_ACCESSION)) {
                    // accession search
                    AccessionSearcher accessionSearcher = new AccessionSearcher(_computeDAO.getCurrentSession());
                    total += accessionSearcher.populateSearchResult(searchTask);
                }
                else if (searchCategory.equals(SearchTask.TOPIC_WEBSITE)) {
                    // website search
                    WebSearcher webSearcher = new WebSearcher(suConnection);
                    total += webSearcher.populateSearchResult(searchTask);
                }
                else if (searchCategory.equals(SearchTask.TOPIC_PROTEIN)) {
                    // protein search  - do not use JDBC
                    total += populateBulkSearchResultsFromLuceneIndexedDocs2(searchTask, searchCategory);
                }
                else {
                    total += populateBulkSearchResultsFromLuceneIndexedDocs2(searchTask, searchCategory);
                    // use JDBC
//                      total+=populateSearchResultsFromLuceneIndexedDocs(searchTask, searchCategory, connection);
                }
                addSubprocessEvent(Event.SUBTASKCOMPLETED_EVENT, searchTask, searchCategory);
            }
        }
        catch (Exception e) {
            addSubprocessEvent(Event.SUBTASKERROR_EVENT, searchTask, tmpSearchCategory);
        }
        finally {
            closeSuConnection();
        }
        return total;
    }

    private void addSubprocessEvent(String eventType, SearchTask task, String topic) throws DaoException {
        Object o = SearchSetup.getSynchObjectForTask(task.getObjectId());
//        _logger.debug("For task " + task.getObjectId() + " synch object hash = " + System.identityHashCode(o));
        synchronized (o) {
            computeBean.saveEvent(task.getObjectId(), eventType, topic, new Date());
        }
    }

    public static String getSearchResultInsertFields() {
        return "docid," +
                "accession," +
                "docname," +
                "category," +
                "doctype," +
                "method," +
                "headline," +
                "rank";
    }

    public static String getSearchCategoryQueryFields() {
        return "docid," +
                "accession," +
                "docname," +
                "category," +
                "doctype," +
                "method," +
                "headline," +
                "rank ";
    }

    public static String getSearchResultValueFields() {
        return ":searchResultNode, " +
                ":docid," +
                ":accession," +
                ":docname," +
                ":doctype" +
                ":headline";
    }

// Keep this around a little bit
//    private int populateSearchResultsFromDBIndexedDocs(SearchTask searchTask, String searchCategory)
//            throws Exception {
//        SearchResultNode searchResultNode = searchTask.getSearchResultNode();
//        if(searchResultNode == null) {
//            throw new IllegalArgumentException("Search task has no result node: " +
//                searchTask.getObjectId());
//        }
//        String sql = "insert into tsearch_ts_result " +
//                     "(" +
//                        "node_id," +
//                        getSearchResultInsertFields() +
//                     ") " +
//                     "select " +
//                        ":searchResultNode," +
//                         getSearchCategoryQueryFields() +
//                     "from " + getSearchQuery(searchCategory);
//        SQLQuery sqlQuery = _computeDAO.getCurrentSession().createSQLQuery(sql);
//        _logger.debug("setCategorySearchQueryParameters() searchResultNode="+searchResultNode.getObjectId()+" searchString="+searchTask.getSearchString()+" matchFlags="+searchTask.getMatchFlagsAsString());
//        setCategorySearchQueryParameters(sqlQuery,
//                searchResultNode.getObjectId(),
//                searchTask.getSearchString(),
//                searchTask.getMatchFlagsAsString());
//        _logger.debug("------ SEARCH ------ executing populateSearchResult() query="+sql);
//        return sqlQuery.executeUpdate();
//    }
//
// Keep this around a little bit
    /**
     * Method which queries Lucene for the hit information
     * @param searchTask - search task submitted
     * @param searchCategory - type of search conducted
     * @return return value of executed update
     * @throws Exception problem trying to populate the search results
     */
//    private int populateSearchResultsFromLuceneIndexedDocs(SearchTask searchTask, String searchCategory) throws Exception {
//        SearchResultNode searchResultNode = searchTask.getSearchResultNode();
//        if(searchResultNode == null) {
//            throw new IllegalArgumentException("Search task has no result node: " +
//                searchTask.getObjectId());
//        }
//        PreparedStatement pstmt = null;
//        Hits hits = null;
//        try {
//            _logger.debug("Searching category '"+searchCategory+"'; searchString="+searchTask.getSearchString()+" matchFlags="+searchTask.getMatchFlagsAsString());
//            // Do the search first
//            LuceneSearcher searcher = LuceneDataFactory.getDocumentSearcher(searchCategory);
//            hits = searcher.search(searchTask.getSearchString());
//            _logger.debug("Found " + hits.length() + " hits for '"+searchCategory+"' search");
//            if (hits.length() > 0)
//            {
//                // After the search is complete do the db work. Connection not open as long.
//                pstmt = suConnection.prepareStatement(searcher.getPreparedStatement());
//                int batchCnt=0;
//                for (Iterator iterator = hits.iterator(); iterator.hasNext();) {
//                    Hit hit =  (Hit)iterator.next();
//                    searcher.prepareStatementForHit(pstmt, hit, searchResultNode.getObjectId(), searchCategory);
//                    pstmt.addBatch();
//                    batchCnt++;
//                    if (batchCnt%MAX_INSERT_BATCH_SIZE==0){
//                        pstmt.executeBatch();
//                    }
//                    // If we hit the max search limit stop.  The best scored hits are returned.
//                    if (batchCnt==MAX_SEARCH_RESULT_SET) { break; }
//                }
//                if (batchCnt%MAX_INSERT_BATCH_SIZE!=0) {pstmt.executeBatch();}
//                suConnection.commit();
//                _logger.debug("Uploading hits to the db completed for "+searchCategory);
//            }
//        }
//        finally {
//            if (null!=pstmt) {
//                pstmt.close();
//            }
//        }
//        return hits.length();
//    }

// Keep this around a little bit
    /**
     * Method uses bulk data load insdead of direct JDBC to load found IDs to the DB
     * @param searchTask - search task submitted
     * @param searchCategory - type of search conducted
     * @return return value of executed update
     * @throws Exception problem populating the bulk search results
     */
//    private int populateBulkSearchResultsFromLuceneIndexedDocs(
//                    SearchTask searchTask, String searchCategory) throws Exception
//    {
//        SearchResultNode searchResultNode = searchTask.getSearchResultNode();
//        if(searchResultNode == null) {
//            throw new IllegalArgumentException("Search task has no result node: " +
//                searchTask.getObjectId());
//        }
//        Long resultNodeID = searchResultNode.getObjectId();
//
//        PreparedStatement pstmt = null;
//        int cnt = 0;
//
//        _logger.debug("Bulk searching category '"+searchCategory+"'; searchString="+searchTask.getSearchString()+" matchFlags="+searchTask.getMatchFlagsAsString());
//        // Do the search first
//        LuceneSearcher searcher = LuceneDataFactory.getDocumentSearcher(searchCategory);
//        Hits hits = searcher.search(searchTask.getSearchString());
//        _logger.debug(searchCategory + ": found " + hits.length() + " lucene hits");
//        if (hits.length() > 0)
//        {
//            String dataFilePath = SystemConfigurationProperties.getString("ts.resultsRootPath") + searchTask.getObjectId() + "_" + searchCategory + ".data";
//            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(dataFilePath)));
//            long outTime = 0, iterTime = 0;
//            Date one, two;
//            for (Iterator iterator = hits.iterator(); iterator.hasNext(); cnt++)
//            {
//                if (cnt==MAX_SEARCH_RESULT_SET) { break; }
//                one = new Date();
//                Hit hit =  (Hit)iterator.next();
//                String outStr = searcher.writeRecordForHit(hit, resultNodeID);
//                two = new Date();
//                iterTime += two.getTime() - one.getTime();
//                out.println(outStr);
//                one = new Date();
//                outTime += one.getTime() - two.getTime();
//                // If we hit the max search limit stop.  The best scored hits are returned.
//            }
//            // close stream
//            out.flush();
//            out.close();
//            _logger.debug(searchCategory + ": finished writing "+ cnt + " hits to the file ");
//            _logger.debug(searchCategory + ": hit iteration time: " + iterTime + " msec; IO time: " + outTime + " msec");
//
//            // load data to DB by using Postgres COPY command
//            // After the search is complete do the db work. Connection not open as long.
//            copyResultsToDB(searcher, dataFilePath, false);
//
//            _logger.debug(searchCategory + ": completed uloading "+ cnt + " hits to the db");
//            File tmpFile = new File(dataFilePath);
//            tmpFile.delete();
//        }
//        return cnt;
//
//    }

    //
    /**
     * Method uses bulk data load insdead of direct JDBC to load found IDs to the DB
     *
     * @param searchTask     - search task submitted
     * @param searchCategory - type of search conducted
     * @return return value of executed update
     * @throws Exception problem populating the bulk search results
     */
    private int populateBulkSearchResultsFromLuceneIndexedDocs2(
            SearchTask searchTask, String searchCategory) throws Exception {
        SearchResultNode searchResultNode = searchTask.getSearchResultNode();
        if (searchResultNode == null) {
            throw new IllegalArgumentException("Search task has no result node: " +
                    searchTask.getObjectId());
        }
        Long resultNodeID = searchResultNode.getObjectId();

        int cnt = 0;

        _logger.debug("Bulk searching category '" + searchCategory + "'; searchString=" + searchTask.getSearchString() + " matchFlags=" + searchTask.getMatchFlagsAsString());
        // Do the search first
        LuceneSearcher searcher = LuceneDataFactory.getDocumentSearcher(searchCategory);
        TopDocs topDocs = searcher.search(searchTask.getSearchString(), MAX_SEARCH_RESULT_SET);
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        _logger.debug(searchCategory + ": found " + scoreDocs.length + " lucene hits");
        if (scoreDocs.length > 0) {
            String dataFilePath = SystemConfigurationProperties.getString("ts.resultsRootPath") + searchTask.getObjectId() + "_" + searchCategory + ".data";
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(dataFilePath)));
            long outTime = 0, iterTime = 0;
            Date one, two;
            try {
                for (ScoreDoc scoreDoc : scoreDocs) {
                    //                    if (cnt==MAX_SEARCH_RESULT_SET) { break; }
                    one = new Date();
                    String outStr = searcher.writeRecordForDoc(scoreDoc, resultNodeID);
                    two = new Date();
                    iterTime += two.getTime() - one.getTime();
                    out.println(outStr);
                    one = new Date();
                    outTime += one.getTime() - two.getTime();
                    // If we hit the max search limit stop.  The best scored hits are returned.
                    cnt++;
                }
            }
            finally {
                // close stream
                out.flush();
                out.close();
            }
            _logger.debug(searchCategory + ": finished writing " + cnt + " hits to the file ");
            _logger.debug(searchCategory + ": hit iteration time: " + iterTime + " msec; IO time: " + outTime + " msec");

            // load data to DB by using Postgres COPY command
            // After the search is complete do the db work. Connection not open as long.
            copyResultsToDB(searcher, dataFilePath, false);

            _logger.debug(searchCategory + ": completed uloading " + cnt + " hits to the db");
            File tmpFile = new File(dataFilePath);
            tmpFile.delete();
        }
        return cnt;

    }

    public void copyResultsToDB(LuceneSearcher searcher, String dataFile, boolean forceCloseConnection) throws SQLException, ClassNotFoundException {
        initSuConnection();
        // load data to DB by using Postgres COPY command
        // After the search is complete do the db work. Connection not open as long.
        PreparedStatement pstmt = null;
        try {
            String sql = searcher.getCopyCommand(dataFile);
            pstmt = suConnection.prepareStatement(sql);
            pstmt.execute();
            suConnection.commit();
        }
        finally {
            if (null != pstmt) {
                pstmt.close();
            }
            if (forceCloseConnection) {
                closeSuConnection();
            }

        }
    }

}
