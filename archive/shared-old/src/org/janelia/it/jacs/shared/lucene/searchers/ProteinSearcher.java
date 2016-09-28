
package src.org.janelia.it.jacs.shared.lucene.searchers;

import org.janelia.it.jacs.model.tasks.search.SearchTask;
import src.org.janelia.it.jacs.shared.lucene.LuceneIndexer;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Mar 6, 2008
 * Time: 1:13:04 PM
 */
public class ProteinSearcher extends LuceneSearcherBase {

    public ProteinSearcher() throws IOException {
        super();
    }

    public String getSearcherIndexType() {
        return LuceneIndexer.INDEX_PROTEINS;
    }

    public String getSearchTaskTopic() {
        return SearchTask.TOPIC_PROTEIN;
    }

    protected String getResultTableName() {
        return "protein_ts_result";
    }

    protected String getIdFieldName() {
        return "oid";
    }


}
