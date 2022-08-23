package org.janelia.workstation.site.hortacloud.quicksearch;

import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.browser.gui.support.FilterQuickSearch;
import org.netbeans.spi.quicksearch.SearchProvider;
import org.netbeans.spi.quicksearch.SearchRequest;
import org.netbeans.spi.quicksearch.SearchResponse;

/**
 * Provider for searching TmWorkspaces with the quick search bar.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TmWorkspaceSearchProvider implements SearchProvider {

    @Override
    public void evaluate(SearchRequest request, SearchResponse response) {
        FilterQuickSearch filterQuickSearch = new FilterQuickSearch(TmWorkspace.class, "workspaces");
        filterQuickSearch.evaluate(request, response);
    }
}
