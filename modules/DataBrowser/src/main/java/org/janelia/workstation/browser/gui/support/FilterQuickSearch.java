package org.janelia.workstation.browser.gui.support;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.gui.search.Filter;
import org.janelia.workstation.browser.actions.NewFilterActionListener;
import org.janelia.workstation.browser.gui.editor.FilterEditorPanel;
import org.janelia.workstation.core.model.search.DomainObjectSearchResults;
import org.janelia.workstation.core.model.search.SearchConfiguration;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.netbeans.spi.quicksearch.SearchRequest;
import org.netbeans.spi.quicksearch.SearchResponse;

/**
 * Helper class for implementing a NetBeans quick search provider using a domain object filter.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FilterQuickSearch {

    private Class<? extends DomainObject> domainClass;
    private String pluralName;

    public FilterQuickSearch(Class<? extends DomainObject> domainClass, String pluralName) {
        this.domainClass = domainClass;
        this.pluralName = pluralName;
    }

    public void evaluate(SearchRequest request, SearchResponse response) {
        String searchString = request.getText();

        Filter filter = FilterEditorPanel.createUnsavedFilter(domainClass, null);
        filter.setSearchString(searchString);
        SearchConfiguration searchConfig = new SearchConfiguration(filter, DomainObjectSearchResults.PAGE_SIZE);
        long numResults;
        try {
            numResults = searchConfig.performSearch().getNumTotalResults();
            if (numResults==0) {
                return;
            }
        }
        catch (Exception e) {
            FrameworkAccess.handleExceptionQuietly(e);
            return;
        }

        String title = String.format("Found %d %s containing '%s'. Click here to view.",
                numResults, pluralName, searchString);

        // Ignore return value, because we only give one result
        response.addResult(new OpenNewFilter(searchString), title);
    }

    private class OpenNewFilter implements Runnable {

        private String searchString;

        public OpenNewFilter(String searchString) {
            this.searchString = searchString;
        }

        @Override
        public void run() {
            NewFilterActionListener listener = new NewFilterActionListener(searchString, domainClass);
            listener.actionPerformed(null);
        }
    }
}
