
package src.org.janelia.it.jacs.compute.mbean;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 15, 2006
 * Time: 1:04:46 PM
 */
public interface SearchDataManagerMBean {

    public void updateAllIndices();

    public void updateIndexForClusterData();

    public void updateIndexForProjectData();

    public void updateIndexForProteinData();

    public void updateIndexForPublicationData();

    public void updateIndexForSampleData();

    public void searchPublication(String searchString);

    public void searchProject(String searchString);

    public void searchProtein(String searchString);

    public void searchFinalCluster(String searchString);

    public void searchSamples(String searchString);

    public void searchEntities(String searchString);
}