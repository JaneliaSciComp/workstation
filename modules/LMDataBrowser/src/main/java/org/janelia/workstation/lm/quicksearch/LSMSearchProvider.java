package org.janelia.workstation.lm.quicksearch;

import org.janelia.model.domain.sample.LSMImage;
import org.janelia.workstation.browser.gui.support.FilterQuickSearch;
import org.netbeans.spi.quicksearch.SearchProvider;
import org.netbeans.spi.quicksearch.SearchRequest;
import org.netbeans.spi.quicksearch.SearchResponse;

/**
 * Provider for searching LSMImages with the quick search bar.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class LSMSearchProvider implements SearchProvider {

    public void evaluate(SearchRequest request, SearchResponse response) {
        FilterQuickSearch filterQuickSearch = new FilterQuickSearch(LSMImage.class, "LSM images");
        filterQuickSearch.evaluate(request, response);
    }
}
