package org.janelia.it.FlyWorkstation.api.facade.abstract_facade;

import org.apache.solr.client.solrj.SolrQuery;
import org.janelia.it.jacs.compute.api.support.SolrResults;

/**
 * Facade interface for SOLR searching.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface SolrFacade {

	public SolrResults searchSolr(SolrQuery query) throws Exception;
	
}
