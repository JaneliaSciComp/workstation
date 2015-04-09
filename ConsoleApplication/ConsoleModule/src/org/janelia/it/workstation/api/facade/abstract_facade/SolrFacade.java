package org.janelia.it.workstation.api.facade.abstract_facade;

import org.apache.solr.client.solrj.SolrQuery;
import org.janelia.it.jacs.shared.solr.SageTerm;
import org.janelia.it.jacs.shared.solr.SolrResults;

import java.util.Map;

/**
 * Facade interface for SOLR searching.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface SolrFacade {

    public SolrResults searchSolr(SolrQuery query, boolean mapToEntities) throws Exception;

    public SolrResults searchSolr(SolrQuery query) throws Exception;

    public Map<String, SageTerm> getImageVocabulary() throws Exception;

}
