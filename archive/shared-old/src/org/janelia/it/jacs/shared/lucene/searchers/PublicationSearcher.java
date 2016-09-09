
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
public class PublicationSearcher extends LuceneSearcherBase {

    public PublicationSearcher() throws IOException {
        super();
    }

    public String getSearcherIndexType() {
        return LuceneIndexer.INDEX_PUBLICATIONS;
    }

    public String getSearchTaskTopic() {
        return SearchTask.TOPIC_PUBLICATION;
    }

    protected String getResultTableName() {
        return "publication_ts_result";
    }

    protected String getIdFieldName() {
        return "oid";
    }

}