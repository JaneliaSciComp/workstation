package org.janelia.it.workstation.browser.gui.colordepth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.janelia.it.workstation.browser.model.search.SearchResults;
import org.janelia.model.domain.gui.colordepth.ColorDepthMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages a set of domain search results with pagination.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthSearchResults implements SearchResults<ColorDepthMatch, String> {

    private static final Logger log = LoggerFactory.getLogger(ColorDepthSearchResults.class);

    protected final List<ColorDepthResultPage> pages = new ArrayList<>();
    protected Set<Integer> loadedPages = new HashSet<>();
    protected long numTotalResults = 0;
    protected long numLoadedResults = 0;
    
    /**
     * Factory method to paginate a list of results already in memory. 
     * @param domainObjects
     * @return 
     */
    public ColorDepthSearchResults(Collection<ColorDepthMatch> matches) {
        
        List<ColorDepthMatch> pageObjects = new ArrayList<>();
        
        for(ColorDepthMatch match : matches)  {
            if (match==null) continue;
            pageObjects.add(match);
            if (pageObjects.size() >= SearchResults.PAGE_SIZE) {
                addPage(createResultPage(pageObjects, matches.size()));
                pageObjects.clear();
            }
        }
        
        if (!pageObjects.isEmpty()) {
            // Create one more page with the remaining items
            addPage(createResultPage(pageObjects, matches.size()));
        }
        
        if (getPages().isEmpty()) {
            // Construct an empty search results so as not to return null from this factory method
            addPage(createResultPage(new ArrayList<ColorDepthMatch>(), 0));
        }
    }
    
    public ColorDepthSearchResults(ColorDepthResultPage firstPage) {
        addPage(firstPage);
    }

    final void addPage(ColorDepthResultPage resultPage) {
        updateNumResults(resultPage);
        pages.add(resultPage);
        loadedPages.add(pages.size()-1);
    }
    
    final void setPage(int page, ColorDepthResultPage resultPage) {
        updateNumResults(resultPage);
        while (pages.size()-1<page) {
            pages.add(null);
        }
        pages.set(page, resultPage);
        loadedPages.add(page);
    }
    
    @Override
    public int getNumLoadedPages() {
        return loadedPages.size();
    }

    @Override
    public int getNumTotalPages() {
        return (int)Math.ceil((double)numTotalResults / (double)PAGE_SIZE);
    }

    @Override
    public long getNumLoadedResults() {
        return numLoadedResults;
    }

    @Override
    public long getNumTotalResults() {
        return numTotalResults;
    }

    @Override
    public boolean hasMoreResults() {
        return getNumLoadedPages()<getNumTotalPages();
    }

    @Override
    public List<ColorDepthResultPage> getPages() {
        return pages;
    }

    @Override
    public ColorDepthResultPage getPage(int page) throws Exception {
        if (page>pages.size()-1 || page<0) {
            return null;
        }
        return pages.get(page);
    }

    @Override
    public boolean isAllLoaded() {
        return getNumTotalPages()==getNumLoadedPages();
    }
    
    private void updateNumResults(ColorDepthResultPage resultPage) {
        if (!pages.isEmpty() && numTotalResults!=resultPage.getNumTotalResults()) {
            log.warn("Adding page where total number of results ({}) does not match result set ({}) ",resultPage.getNumTotalResults(),numTotalResults);
        }
        
        numLoadedResults += resultPage.getNumPageResults();
        numTotalResults = resultPage.getNumTotalResults();
        log.debug("Updated numLoaded to {}", numLoadedResults);
        log.debug("Updated numFound to {}", numTotalResults);
    }

    public boolean updateIfFound(ColorDepthMatch match) {

        boolean updated = false;
        for(final ColorDepthResultPage page : getPages()) {
            if (page==null) continue; // Page not yet loaded
            final ColorDepthMatch pageObject = page.getObjectById(match.getFilepath());
            if (pageObject!=null) {
                page.updateObject(match);
                updated = true;
            }
        }
        
        return updated;
    }
    
    private final ColorDepthResultPage createResultPage(List<ColorDepthMatch> matches, long totalNumResults) {
        return new ColorDepthResultPage(matches, totalNumResults);
    }
}
