package org.janelia.workstation.lm.quicksearch;

import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.browser.gui.support.FilterQuickSearch;
import org.netbeans.spi.quicksearch.SearchProvider;
import org.netbeans.spi.quicksearch.SearchRequest;
import org.netbeans.spi.quicksearch.SearchResponse;

/**
 * Provider for searching Samples with the quick search bar.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleSearchProvider implements SearchProvider {

    public void evaluate(SearchRequest request, SearchResponse response) {
        FilterQuickSearch filterQuickSearch = new FilterQuickSearch(Sample.class, "samples");
        filterQuickSearch.evaluate(request, response);
    }
}
