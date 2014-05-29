package org.janelia.it.workstation.api.facade.concrete_facade.ejb;

import org.apache.solr.client.solrj.SolrQuery;
import org.janelia.it.jacs.shared.solr.SageTerm;
import org.janelia.it.jacs.shared.solr.SolrResults;

import java.util.Map;

/**
 * Facade for running SOLR searches using the remote EJB's. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EJBSolrFacade extends EJBEntityFacade implements org.janelia.it.workstation.api.facade.abstract_facade.SolrFacade {

	public SolrResults searchSolr(SolrQuery query) throws Exception {
		return EJBFactory.getRemoteSolrBean().search(org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSubjectKey(), query, true);
	}
	
	public Map<String, SageTerm> getFlyLightVocabulary() throws Exception {
		return EJBFactory.getRemoteSolrBean().getFlyLightVocabulary();
	}
	
}
