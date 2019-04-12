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

    public static final int PAGE_SIZE = 500;
    
    public int getNumLoadedPages();

    public int getNumTotalPages();
    
    public long getNumLoadedResults();

    public long getNumTotalResults();

    public boolean hasMoreResults();

    public boolean isAllLoaded();
    
    public List<? extends ResultPage<T,S>> getPages();
    
    public ResultPage<T,S> getPage(int page) throws Exception;

    public boolean updateIfFound(T object);
   
}
