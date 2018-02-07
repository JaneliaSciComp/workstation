package org.janelia.it.workstation.browser.gui.support;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.browser.gui.editor.FilterEditorPanel;
import org.janelia.it.workstation.browser.model.search.SearchConfiguration;
import org.janelia.it.workstation.browser.model.search.DomainObjectSearchResults;
import org.janelia.it.workstation.browser.nb_action.NewFilterActionListener;
import org.janelia.model.domain.gui.search.Filter;
import org.janelia.model.domain.sample.LSMImage;
import org.netbeans.spi.quicksearch.SearchProvider;
import org.netbeans.spi.quicksearch.SearchRequest;
import org.netbeans.spi.quicksearch.SearchResponse;

public class LSMSearchProvider implements SearchProvider {

    public void evaluate(SearchRequest request, SearchResponse response) {                
        String searchString = request.getText();

        Filter filter = FilterEditorPanel.createUnsavedFilter(LSMImage.class, null);
        filter.setSearchString(searchString);
        SearchConfiguration searchConfig = new SearchConfiguration(filter, DomainObjectSearchResults.PAGE_SIZE);
        Long numResults = null;
        try {
            numResults = searchConfig.performSearch().getNumTotalResults();
            if (numResults==0) {
                return;
            }
        }
        catch (Exception e) {
            FrameworkImplProvider.handleExceptionQuietly(e);
        }
        
        String title;
        if (numResults!=null) {
            title = String.format("Found %d LSMs containing '%s'. Click here to view.", numResults, searchString);
        }
        else {
            title = String.format("Click here to search for LSMs containing '%s'.", searchString);
        }

        if (!response.addResult(new OpenNewFilter(searchString), title)) {
            return;
        }
    }
    
    private static class OpenNewFilter implements Runnable {

        private String searchString;

        public OpenNewFilter(String searchString) {
            this.searchString = searchString;
        }

        @Override
        public void run() {
            NewFilterActionListener listener = new NewFilterActionListener(searchString, LSMImage.class);
            listener.actionPerformed(null);
        }
    }

}
