package org.janelia.it.workstation.gui.browser.search;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages a set of domain search results with pagination.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SearchResults {

    private static final Logger log = LoggerFactory.getLogger(SearchResults.class);

    public static final int PAGE_SIZE = 500;
    
    protected final List<ResultPage> pages = new ArrayList<>();
    protected int numTotalResults = 0;
    protected int numLoadedResults = 0;

    public SearchResults(ResultPage firstPage) {
        addPage(firstPage);
    }
    
    public int getNumLoadedPages() {
        return pages.size();
    }

    public int getNumTotalPages() {
        return (int)Math.ceil((double)numTotalResults / (double)PAGE_SIZE);
    }
    
    public int getNumLoadedResults() {
        return numLoadedResults;
    }

    public int getNumTotalResults() {
        return numTotalResults;
    }

    public boolean hasMoreResults() {
        return getNumLoadedPages()<getNumTotalPages();
    }

    public List<ResultPage> getPages() {
        return pages;
    }

    public void clear() {
        pages.clear();
        numLoadedResults = 0;
    }
    
    public ResultPage getPage(int page) {
        if (page>pages.size()-1 || page<0) {
            return null;
        }
        return pages.get(page);
    }

    public final void addPage(ResultPage resultPage) {
        updateNumResults(resultPage);
        pages.add(resultPage);
    }
    
    public final void setPage(int page, ResultPage resultPage) {
        updateNumResults(resultPage);
        while (pages.size()-1<page) {
            pages.add(null);
        }
        pages.set(page, resultPage);
    }
    
    private void updateNumResults(ResultPage resultPage) {
        if (!pages.isEmpty() && numTotalResults!=resultPage.getNumTotalResults()) {
            log.warn("Adding page where total number of results ({}) does not match result set ({}) ",resultPage.getNumTotalResults(),numTotalResults);
        }
        
        numLoadedResults += resultPage.getNumPageResults();
        numTotalResults = resultPage.getNumTotalResults();
        log.debug("Updated numLoaded to {}", numLoadedResults);
        log.debug("Updated numFound to {}", numTotalResults);
    }
}
