package org.janelia.workstation.core.model.search;

import java.util.List;

/**
 * Paginated search results of a certain type.
 * 
 * T - type of the result objects
 * S - type of the unique identifier for the results
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface SearchResults<T,S> {

    int PAGE_SIZE = 500;
    
    int getNumLoadedPages();

    int getNumTotalPages();
    
    long getNumLoadedResults();

    long getNumTotalResults();

    boolean hasMoreResults();

    boolean isAllLoaded();
    
    List<? extends ResultPage<T,S>> getPages();
    
    ResultPage<T,S> getPage(int page) throws Exception;

    boolean updateIfFound(T object);
   
}
