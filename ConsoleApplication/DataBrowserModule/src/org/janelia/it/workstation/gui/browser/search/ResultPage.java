package org.janelia.it.workstation.gui.browser.search;

import java.util.*;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.shared.solr.SolrResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One page of results, treated as a unit for performance reasons.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ResultPage {

    private static final Logger log = LoggerFactory.getLogger(ResultPage.class);

    private SolrResults solrResults;
    private Map<Long, DomainObject> domainObjectById = new HashMap<Long, DomainObject>();
    private List<ResultItem> resultItems;
    
    public ResultPage(SolrResults solrResults) {
        // TODO: only keep what we need from the SolrResults and discard it
        this.solrResults = solrResults;
    }

    public long getTotalNumItems() {
        return solrResults.getResponse().getResults().getNumFound();
    }
    
    public long getNumItems() {
        return solrResults.getResponse().getResults().size();
    }
    
    public DomainObject getDomainObject(Long id) {
        return domainObjectById.get(id);
    }

    public List<ResultItem> getResultItems() {
        if (resultItems==null) {
            resultItems = new ArrayList<ResultItem>();
            SolrDocumentList docList = solrResults.getResponse().getResults();
            for(Iterator iterator=docList.iterator(); iterator.hasNext(); ) {
                SolrDocument doc = (SolrDocument)iterator.next();
                resultItems.add(new ResultItem(doc.getFieldValueMap()));
            }
        }
        return resultItems;
    }
}
