package org.janelia.it.workstation.gui.browser.model.search;

import java.util.List;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Search results backed by a SOLR search.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SolrSearchResults extends SearchResults {

    private static final Logger log = LoggerFactory.getLogger(SolrSearchResults.class);
    
    private final SearchConfiguration searchConfig;
    
    public SolrSearchResults(SearchConfiguration searchConfig, ResultPage firstPage) {
        super(firstPage);
        this.searchConfig = searchConfig;
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
    
    @Override
    public List<ResultPage> getPages() {
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
            SessionMgr.getSessionMgr().handleException(e);
        }
    }
    
}
