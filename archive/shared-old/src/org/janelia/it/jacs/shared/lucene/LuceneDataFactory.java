
/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Mar 5, 2008
 * Time: 6:00:46 PM
 *
 */
package src.org.janelia.it.jacs.shared.lucene;

import src.org.janelia.it.jacs.shared.lucene.data_retrievers.*;
import src.org.janelia.it.jacs.shared.lucene.searchers.*;

import java.io.IOException;

public class LuceneDataFactory {

    public static LuceneDataRetrieverBase createDocumentRetriever(String docType) {
        if (LuceneIndexer.SET_OF_ALL_DOC_TYPES.contains(docType)) {
            if (docType.equals(LuceneIndexer.INDEX_CLUSTERS)) {
                return new ClustersDataRetriever();
            }
            else if (docType.equals(LuceneIndexer.INDEX_PROJECTS)) {
                return new ProjectsDataRetriever();
            }
            else if (docType.equals(LuceneIndexer.INDEX_PROTEINS)) {
                return new ProteinsDataRetriever();
            }
            else if (docType.equals(LuceneIndexer.INDEX_PUBLICATIONS)) {
                return new PublicationsDataRetriever();
            }
            else if (docType.equals(LuceneIndexer.INDEX_SAMPLES)) {
                return new SamplesDataRetriever();
            }
            else if (docType.equals(LuceneIndexer.INDEX_ENITTIES)) {
                return new EntityDataRetriever();
            }
        }

        return null;
    }

    public static LuceneSearcherBase getDocumentSearcher(String docType) throws IOException {
        if (LuceneIndexer.SET_OF_ALL_DOC_TYPES.contains(docType)) {
            if (LuceneIndexer.INDEX_CLUSTERS.equals(docType)) {
                return new ClusterSearcher();
            }
            else if (LuceneIndexer.INDEX_PROJECTS.equals(docType)) {
                return new ProjectsSearcher();
            }
            else if (LuceneIndexer.INDEX_PROTEINS.equals(docType)) {
                return new ProteinSearcher();
            }
            else if (LuceneIndexer.INDEX_PUBLICATIONS.equals(docType)) {
                return new PublicationSearcher();
            }
            else if (LuceneIndexer.INDEX_SAMPLES.equals(docType)) {
                return new SampleSearcher();
            }
            else if (LuceneIndexer.INDEX_ENITTIES.equals(docType)) {
                return new EntitySearcher();
            }
        }

        return null;
    }

}
