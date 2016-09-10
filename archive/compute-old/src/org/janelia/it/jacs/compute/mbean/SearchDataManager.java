
package src.org.janelia.it.jacs.compute.mbean;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import src.org.janelia.it.jacs.shared.lucene.LuceneIndexer;

import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jul 2, 2007
 * Time: 5:00:41 PM
 */
public class SearchDataManager implements SearchDataManagerMBean {

    private static final Logger LOGGER = Logger.getLogger(SearchDataManager.class);

    public SearchDataManager() {
    }

    public void updateAllIndices() {
        updateIndexForData(LuceneIndexer.INDEX_ALL);
    }

    public void updateIndexForClusterData() {
        updateIndexForData(LuceneIndexer.INDEX_CLUSTERS);
    }

    public void updateIndexForProjectData() {
        updateIndexForData(LuceneIndexer.INDEX_PROJECTS);
    }

    public void updateIndexForProteinData() {
        updateIndexForData(LuceneIndexer.INDEX_PROTEINS);
    }

    public void updateIndexForPublicationData() {
        updateIndexForData(LuceneIndexer.INDEX_PUBLICATIONS);
    }

    public void updateIndexForSampleData() {
        updateIndexForData(LuceneIndexer.INDEX_SAMPLES);
    }

    public void updateIndexForEntityData() {
        updateIndexForData(LuceneIndexer.INDEX_ENITTIES);
    }

    private void updateIndexForData(String dataType) {
        LOGGER.debug("Updating Lucene index file for data type=" + dataType);
        try {
            LuceneIndexer indexer = new LuceneIndexer();
            Set<String> tmpDocTypeSet;
            if (LuceneIndexer.INDEX_ALL.equals(dataType)) {
                tmpDocTypeSet = LuceneIndexer.SET_OF_ALL_DOC_TYPES;
            }
            else {
                tmpDocTypeSet = new HashSet<String>();
                tmpDocTypeSet.add(dataType);
            }
            indexer.execute(tmpDocTypeSet, Integer.MAX_VALUE);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Search Methods
     */
    public void searchPublication(String searchString) {
        try {
            EJBFactory.getRemoteSearchBean().search(LuceneIndexer.INDEX_PUBLICATIONS, searchString);
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void searchProject(String searchString) {
        try {
            EJBFactory.getRemoteSearchBean().search(LuceneIndexer.INDEX_PROJECTS, searchString);
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void searchFinalCluster(String searchString) {
        try {
            EJBFactory.getRemoteSearchBean().search(LuceneIndexer.INDEX_CLUSTERS, searchString);
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void searchProtein(String searchString) {
        try {
            EJBFactory.getRemoteSearchBean().search(LuceneIndexer.INDEX_PROTEINS, searchString);
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void searchSamples(String searchString) {
        try {
            EJBFactory.getRemoteSearchBean().search(LuceneIndexer.INDEX_SAMPLES, searchString);
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void searchEntities(String searchString) {
        try {
            EJBFactory.getRemoteSearchBean().search(LuceneIndexer.INDEX_ENITTIES, searchString);
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}