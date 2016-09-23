
package src.org.janelia.it.jacs.shared.lucene.searchers;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.*;
import src.org.janelia.it.jacs.shared.lucene.LuceneIndexer;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Apr 24, 2008
 * Time: 1:31:43 PM
 */
public abstract class LuceneSearcherBase implements LuceneSearcher {

    protected Logger _logger = Logger.getLogger(this.getClass());

    abstract public String getSearcherIndexType();

    abstract public String getSearchTaskTopic();

    abstract protected String getResultTableName();

    abstract protected String getIdFieldName();

    IndexSearcher luceneSearcher;

    public LuceneSearcherBase() throws IOException {
        try {
            luceneSearcher = new IndexSearcher(LuceneIndexer.getIndexRootPath() + getSearcherIndexType());
            _logger.debug("Lucene searcher instantiated");
        }
        catch (IOException e) {
            _logger.error("Unable to instantiate lucene searcher");
            throw e;
        }
    }

    /**
     * This method searches for the given string within the Lucene Document index file for the subject area in
     * question.
     *
     * @param searchString - look for this string in the Documents
     * @return - Hits of where the string was found
     * @throws IOException    - could not look in and access the necessary files
     * @throws ParseException - had trouble parsing the information
     */
    public Hits search(String searchString) throws IOException, ParseException {
        Analyzer analyzer = new StandardAnalyzer();
        QueryParser qp = new QueryParser("content", analyzer);
        qp.setDefaultOperator(QueryParser.Operator.AND);
        Query query = qp.parse(searchString);
//        _logger.debug("\n\nExecuting search for: "+searchString);
        //        _logger.debug("Found " + hits.length() + " hits");
        return luceneSearcher.search(query);
    }

    public TopDocs search(String searchString, int maxDocs) throws IOException, ParseException {
        Analyzer analyzer = new StandardAnalyzer();
        QueryParser qp = new QueryParser("content", analyzer);
        qp.setDefaultOperator(QueryParser.Operator.AND);
        Query query = qp.parse(searchString);
//        _logger.debug("\n\nExecuting search for: "+searchString);
        //        _logger.debug("Found " + hits.length() + " hits");
        return luceneSearcher.search(query, null, maxDocs);
    }

    /* all temp tables are the same */
    public String getPreparedStatement() {
        return "insert into " + getResultTableName() + " (node_id, hit_id, rank) values (?,?,?)";
    }

    public void prepareStatementForHit(PreparedStatement pstmt, Hit hit, Long resultNodeId, String searchCategory)
            throws SQLException, IOException {
        pstmt.setLong(1, resultNodeId);                      // node
        pstmt.setLong(2, Long.valueOf(hit.get("oid")));       // oid
        pstmt.setFloat(3, hit.getScore());                    // rank
    }

    public String writeRecordForHit(Hit hit, Long resultNodeId) throws IOException {
        return (resultNodeId + "\t" + hit.get(getIdFieldName()) + "\t" + hit.getScore());
    }

    public String writeRecordForDoc(ScoreDoc scoreDoc, Long resultNodeId) throws IOException {
        Document doc = luceneSearcher.doc(scoreDoc.doc);
        return (resultNodeId + "\t" + doc.get(getIdFieldName()) + "\t" + scoreDoc.score);
    }

    public String getCopyCommand(String path) {
        return "copy flyportal." + getResultTableName() + "(node_id,hit_id,rank) from '" + path + "'";
    }

}
