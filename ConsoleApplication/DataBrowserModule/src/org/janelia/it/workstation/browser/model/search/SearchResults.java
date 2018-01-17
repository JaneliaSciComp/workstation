package org.janelia.it.workstation.browser.model.search;

import java.util.List;

public interface SearchResults<T,S> {

    public static final int PAGE_SIZE = 500;
    
    public int getNumLoadedPages();

    public int getNumTotalPages();
    
    public long getNumLoadedResults();

    public long getNumTotalResults();

    public boolean hasMoreResults();

    public boolean isAllLoaded();

    public void loadAllResults();
    
    public List<? extends ResultPage<T,S>> getPages();
    
    public ResultPage<T,S> getPage(int page) throws Exception;

    public boolean updateIfFound(T object);
}
