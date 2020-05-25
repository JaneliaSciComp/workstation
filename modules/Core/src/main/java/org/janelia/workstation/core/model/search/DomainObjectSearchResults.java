package org.janelia.workstation.core.model.search;

import com.google.common.collect.ListMultimap;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.ontology.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Manages a set of domain search results with pagination.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectSearchResults implements SearchResults<DomainObject, Reference> {

    private static final Logger log = LoggerFactory.getLogger(DomainObjectSearchResults.class);

    protected final List<DomainObjectResultPage> pages = new ArrayList<>();
    protected Set<Integer> loadedPages = new HashSet<>();
    protected long numTotalResults = 0;
    protected long numLoadedResults = 0;
    
    /**
     * Constructor which paginates a list of results already in memory.
     * @param domainObjects
     * @param annotations
     * @return 
     */
    public DomainObjectSearchResults(Collection<? extends DomainObject> domainObjects, Collection<Annotation> annotations) {
        
        List<DomainObject> pageObjects = new ArrayList<>();
        ListMultimap<Reference, Annotation> annotationsByTarget = DomainUtils.getAnnotationsByDomainObjectReference(annotations);
        Set<DomainObject> uniqueObjects = new LinkedHashSet<>(domainObjects);
        Set<Annotation> pageAnnotations = new LinkedHashSet<>();

        // Paginate results
        for(DomainObject domainObject : uniqueObjects)  {
            if (domainObject==null) continue;
            pageObjects.add(domainObject);
            List<Annotation> objectAnnotations = annotationsByTarget.get(Reference.createFor(domainObject));
            pageAnnotations.addAll(objectAnnotations);
            if (pageObjects.size() >= PAGE_SIZE) {
                addPage(createResultPage(pageObjects, new ArrayList<>(pageAnnotations), domainObjects.size()));
                pageObjects.clear();
                pageAnnotations.clear();
            }
        }
        
        if (!pageObjects.isEmpty()) {
            // Create one more page with the remaining items
            addPage(createResultPage(pageObjects, new ArrayList<>(pageAnnotations), domainObjects.size()));
        }
        
        if (getPages().isEmpty()) {
            // Construct an empty search results so as not to return null from this factory method
            addPage(createResultPage(new ArrayList<>(), new ArrayList<>(), 0));
        }
    }

    /**
     * Constructor with the first page of results.
     * @param firstPage
     */
    public DomainObjectSearchResults(DomainObjectResultPage firstPage) {
        addPage(firstPage);
    }

    final void addPage(DomainObjectResultPage resultPage) {
        updateNumResults(resultPage);
        pages.add(resultPage);
        loadedPages.add(pages.size()-1);
    }
    
    protected final void setPage(int page, DomainObjectResultPage resultPage) {
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
    public List<DomainObjectResultPage> getPages() {
        return pages;
    }

    @Override
    public DomainObjectResultPage getPage(int page) throws Exception {
        if (page>pages.size()-1 || page<0) {
            return null;
        }
        return pages.get(page);
    }

    @Override
    public boolean isAllLoaded() {
        return getNumTotalPages()==getNumLoadedPages();
    }
    
    private void updateNumResults(DomainObjectResultPage resultPage) {
        if (!pages.isEmpty() && numTotalResults!=resultPage.getNumTotalResults()) {
            log.warn("Adding page where total number of results ({}) does not match result set ({}) ",resultPage.getNumTotalResults(),numTotalResults);
        }
        
        numLoadedResults += resultPage.getNumPageResults();
        numTotalResults = resultPage.getNumTotalResults();
        log.debug("Updated numLoaded to {}", numLoadedResults);
        log.debug("Updated numFound to {}", numTotalResults);
    }

    public boolean updateIfFound(DomainObject domainObject) {

        boolean updated = false;
        for(final DomainObjectResultPage page : getPages()) {
            if (page==null) continue; // Page not yet loaded
            final DomainObject pageObject = page.getObjectById(Reference.createFor(domainObject));
            if (pageObject!=null) {
                page.updateObject(domainObject);
                updated = true;
            }
        }
        
        return updated;
    }
    
    private final DomainObjectResultPage createResultPage(List<DomainObject> domainObjects, List<Annotation> annotations, long totalNumResults) {
        return new DomainObjectResultPage(domainObjects, annotations, totalNumResults);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("pages", pages)
                .append("loadedPages", loadedPages)
                .append("numTotalResults", numTotalResults)
                .append("numLoadedResults", numLoadedResults)
                .toString();
    }
}
