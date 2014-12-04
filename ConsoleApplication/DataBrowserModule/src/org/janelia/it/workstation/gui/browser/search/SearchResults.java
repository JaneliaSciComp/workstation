package org.janelia.it.workstation.gui.browser.search;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages a set of search results, including the loaded pages of Solr results,
 * and the corresponding DomainObjects.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SearchResults {

    private static final Logger log = LoggerFactory.getLogger(SearchResults.class);

    protected final List<ResultPage> pages = new ArrayList<ResultPage>();
    protected final Set<Long> allResultIds = new HashSet<Long>();
    protected final Map<Long, Integer> resultIdToRowIndex = new HashMap<Long, Integer>();

    protected int numFound = 0;
    protected int numLoaded = 0;

    public SearchResults() {
    }

    public int getNumLoadedPages() {
        return pages.size();
    }

    public int getNumResultsLoaded() {
        return numLoaded;
    }

    public int getNumResultsFound() {
        return numFound;
    }

    public boolean hasMoreResults() {
        return numFound > numLoaded && !pages.get(pages.size() - 1).getResultItems().isEmpty();
    }

    public List<ResultPage> getPages() {
        return pages;
    }

    public Integer getRowIndexForResultId(Long entityId) {
        return resultIdToRowIndex.get(entityId);
    }

    public void clear() {
        pages.clear();
        allResultIds.clear();
        resultIdToRowIndex.clear();
        numLoaded = 0;
    }

    public void addPage(ResultPage resultPage) {
        long numOnPage = resultPage.getNumItems();
        pages.add(resultPage);
        for (ResultItem resultItem : resultPage.getResultItems()) {
            if (allResultIds.contains(resultItem.getId())) {
                log.warn("Duplicate id found in results: " + resultItem.getId());
            }
            allResultIds.add(resultItem.getId());
            resultIdToRowIndex.put(resultItem.getId(), allResultIds.size() - 1);
        }
        numLoaded += numOnPage;
        numFound = (int)resultPage.getTotalNumItems();
        log.debug("Updated numLoaded to {}", numLoaded);
        log.debug("Updated numFound to {}", numFound);
    }
}
