package org.janelia.it.workstation.api.facade.abstract_facade;

import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.janelia.it.jacs.compute.api.support.SageTerm;
import org.janelia.it.jacs.compute.api.support.SolrResults;

/**
 * Facade interface for SOLR searching.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface SolrFacade {

	public SolrResults searchSolr(SolrQuery query) throws Exception;
	
	public Map<String, SageTerm> getFlyLightVocabulary() throws Exception;
	
}
