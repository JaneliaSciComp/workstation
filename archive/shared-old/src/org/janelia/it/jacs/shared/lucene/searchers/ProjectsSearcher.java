
package src.org.janelia.it.jacs.shared.lucene.searchers;

import org.apache.lucene.search.Hit;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import src.org.janelia.it.jacs.shared.lucene.LuceneIndexer;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Mar 6, 2008
 * Time: 1:13:04 PM
 */
public class ProjectsSearcher extends LuceneSearcherBase {

    public ProjectsSearcher() throws IOException {
        super();
    }

    public String getSearcherIndexType() {
        return LuceneIndexer.INDEX_PROJECTS;
    }

    public String getSearchTaskTopic() {
        return SearchTask.TOPIC_PROJECT;
    }

    protected String getResultTableName() {
        return "project_ts_result";
    }

    protected String getIdFieldName() {
        return "accession";
    }

    /* overriding base class - projects use accessions instead of oids */
    public void prepareStatementForHit(PreparedStatement pstmt, Hit hit, Long resultNodeId, String searchCategory)
            throws SQLException, IOException {
        pstmt.setLong(1, resultNodeId);         // node
        pstmt.setString(2, hit.get("accession"));// accession
        pstmt.setFloat(3, hit.getScore());       // rank
    }

}