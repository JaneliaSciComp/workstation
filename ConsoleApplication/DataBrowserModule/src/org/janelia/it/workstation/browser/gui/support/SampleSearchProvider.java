package org.janelia.it.workstation.browser.gui.support;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.workstation.browser.gui.editor.FilterEditorPanel;
import org.janelia.it.workstation.browser.model.search.SearchConfiguration;
import org.janelia.it.workstation.browser.model.search.SearchResults;
import org.janelia.it.workstation.browser.nb_action.NewFilterActionListener;
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
        String searchString = request.getText();

        Filter filter = FilterEditorPanel.createUnsavedFilter(Sample.class, null);
        filter.setSearchString(searchString);
        SearchConfiguration searchConfig = new SearchConfiguration(filter, SearchResults.PAGE_SIZE);
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
            title = String.format("Found %d Samples containing '%s'. Click here to view.", numResults, searchString);
        }
        else {
            title = String.format("Click here to search for Samples containing '%s'.", searchString);
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
            NewFilterActionListener listener = new NewFilterActionListener(searchString, Sample.class);
            listener.actionPerformed(null);
        }
    }
}