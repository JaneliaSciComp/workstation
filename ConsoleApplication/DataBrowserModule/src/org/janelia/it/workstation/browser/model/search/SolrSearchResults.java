package org.janelia.it.workstation.browser.model.search;

import java.util.List;

import org.janelia.it.workstation.browser.ConsoleApp;

/**
 * Search results backed by a SOLR search.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SolrSearchResults extends DomainObjectSearchResults {

    private final SearchConfiguration searchConfig;
    
    public SolrSearchResults(SearchConfiguration searchConfig, DomainObjectResultPage firstPage) {
        super(firstPage);
        this.searchConfig = searchConfig;
    }

    @Override
    public DomainObjectResultPage getPage(int page) throws Exception {
        DomainObjectResultPage resultPage = super.getPage(page);
        if (resultPage==null) {
            resultPage = searchConfig.performSearch(page);
            setPage(page, resultPage);
        }
        return resultPage;
    }
    
    @Override
    public List<DomainObjectResultPage> getPages() {
        return pages;
    }

    @Override
    public void loadAllResults() {
        try {
            for(int i=0; i<getNumTotalPages(); i++) {
                getPage(i);
            }
        }
        catch (Exception e) {
            ConsoleApp.handleException(e);
        }
    }
    
}
