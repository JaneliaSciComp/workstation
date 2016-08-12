package org.janelia.it.workstation.gui.browser.model.search;

import com.google.common.collect.ListMultimap;

import java.util.*;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
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

    /**
     * Factory method to paginate a list of results already in memory. 
     * @param domainObjects
     * @param annotations
     * @return 
     */
    public static SearchResults paginate(Collection<? extends DomainObject> domainObjects, Collection<Annotation> annotations) {
        
        SearchResults searchResults = new SearchResults();
        List<DomainObject> pageObjects = new ArrayList<>();
        
        ListMultimap<Long, Annotation> annotationsByTarget = DomainUtils.getAnnotationsByDomainObjectId(annotations);
        Set<DomainObject> uniqueObjects = new LinkedHashSet<>(domainObjects);
        Set<Annotation> pageAnnotations = new LinkedHashSet<>();
        
        for(DomainObject domainObject : uniqueObjects)  {
            if (domainObject==null) continue;
            pageObjects.add(domainObject);
            List<Annotation> annots = annotationsByTarget.get(domainObject.getId());
            pageAnnotations.addAll(annots);
            if (pageObjects.size()>=PAGE_SIZE) {
                searchResults.addPage(new ResultPage(pageObjects, new ArrayList<>(pageAnnotations), domainObjects.size()));
                pageObjects.clear();
                pageAnnotations.clear();
            }
        }
        
        if (!pageObjects.isEmpty()) {
            // Create one more page with the remaining items
            searchResults.addPage(new ResultPage(pageObjects, new ArrayList<>(pageAnnotations), domainObjects.size()));
        }
        
        if (searchResults.getPages().isEmpty()) {
            // Construct an empty search results so as not to return null from this factory method
            searchResults.addPage(new ResultPage(new ArrayList<DomainObject>(), new ArrayList<Annotation>(), 0));
        }
        
        return searchResults;
    }
    
    protected final List<ResultPage> pages = new ArrayList<>();
    protected Set<Integer> loadedPages = new HashSet<>();
    protected long numTotalResults = 0;
    protected long numLoadedResults = 0;

    private SearchResults() {
    }
    
    public SearchResults(ResultPage firstPage) {
        addPage(firstPage);
    }
    
    public int getNumLoadedPages() {
        return loadedPages.size();
    }

    public int getNumTotalPages() {
        return (int)Math.ceil((double)numTotalResults / (double)PAGE_SIZE);
    }
    
    public long getNumLoadedResults() {
        return numLoadedResults;
    }

    public long getNumTotalResults() {
        return numTotalResults;
    }

    public boolean hasMoreResults() {
        return getNumLoadedPages()<getNumTotalPages();
    }

    public List<ResultPage> getPages() {
        return pages;
    }
    
    public ResultPage getPage(int page) throws Exception {
        if (page>pages.size()-1 || page<0) {
            return null;
        }
        return pages.get(page);
    }

    public final void addPage(ResultPage resultPage) {
        updateNumResults(resultPage);
        pages.add(resultPage);
        loadedPages.add(pages.size()-1);
    }
    
    public final void setPage(int page, ResultPage resultPage) {
        updateNumResults(resultPage);
        while (pages.size()-1<page) {
            pages.add(null);
        }
        pages.set(page, resultPage);
        loadedPages.add(page);
    }

    public boolean isAllLoaded() {
        return getNumTotalPages()==getNumLoadedPages();
    }
    
    public void loadAllResults() {
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
