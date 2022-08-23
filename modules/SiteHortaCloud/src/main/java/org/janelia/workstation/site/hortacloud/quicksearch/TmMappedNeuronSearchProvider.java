package org.janelia.workstation.site.hortacloud.quicksearch;

import org.janelia.model.domain.tiledMicroscope.TmMappedNeuron;
import org.janelia.workstation.browser.gui.support.FilterQuickSearch;
import org.netbeans.spi.quicksearch.SearchProvider;
import org.netbeans.spi.quicksearch.SearchRequest;
import org.netbeans.spi.quicksearch.SearchResponse;

/**
 * Provider for searching TmMappedNeurons with the quick search bar.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TmMappedNeuronSearchProvider implements SearchProvider {

    @Override
    public void evaluate(SearchRequest request, SearchResponse response) {
        FilterQuickSearch filterQuickSearch = new FilterQuickSearch(TmMappedNeuron.class, "neurons");
        filterQuickSearch.evaluate(request, response);
    }
}
