package org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb;

import org.apache.solr.client.solrj.SolrQuery;
import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.SolrFacade;
import org.janelia.it.jacs.compute.api.support.SolrResults;

/**
 * Facade for running SOLR searches using the remote EJB's. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EJBSolrFacade extends EJBEntityFacade implements SolrFacade {

	public SolrResults searchSolr(SolrQuery query) throws Exception {
		return EJBFactory.getRemoteSolrBean().search(query, true);
	}
	
}
