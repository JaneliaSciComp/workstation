package org.janelia.it.workstation.gui.browser.model.search;

/**
 * Search results backed by a SOLR search.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SolrSearchResults extends SearchResults {

    private SearchConfiguration searchConfig;
    
    public SolrSearchResults(SearchConfiguration config, ResultPage firstPage) {
        super(firstPage);
    }

    @Override
    public ResultPage getPage(int page) throws Exception {
        ResultPage resultPage = super.getPage(page);
        if (resultPage==null) {
            resultPage = searchConfig.performSearch(page);
            setPage(page, resultPage);
        }
        return resultPage;
    }
    
    
}
