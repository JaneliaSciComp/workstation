package src.org.janelia.it.jacs.shared.lucene.searchers;

import org.janelia.it.jacs.model.tasks.search.SearchTask;
import src.org.janelia.it.jacs.shared.lucene.LuceneIndexer;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 1/27/12
 * Time: 3:27 PM
 */
public class EntitySearcher extends LuceneSearcherBase {
    public EntitySearcher() throws IOException {
        super();
    }

    public String getSearcherIndexType() {
        return LuceneIndexer.INDEX_ENITTIES;
    }

    public String getSearchTaskTopic() {
        return SearchTask.TOPIC_ENTITIES;
    }

    protected String getResultTableName() {
        return "entities_ts_result";
    }

    protected String getIdFieldName() {
        return "id";
    }


}
