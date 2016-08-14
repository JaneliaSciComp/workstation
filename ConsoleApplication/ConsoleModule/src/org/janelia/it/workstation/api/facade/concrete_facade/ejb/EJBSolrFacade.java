package org.janelia.it.workstation.api.facade.concrete_facade.ejb;

import org.apache.solr.client.solrj.SolrQuery;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.shared.solr.SageTerm;
import org.janelia.it.jacs.shared.solr.SolrResults;
import org.janelia.it.workstation.api.facade.abstract_facade.SolrFacade;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;

import java.util.Map;

/**
 * Facade for running SOLR searches using the remote EJB's. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EJBSolrFacade extends EJBEntityFacade implements SolrFacade {

	public SolrResults searchSolr(SolrQuery query, boolean mapToEntities) throws Exception {
		return EJBFactory.getRemoteSolrBean().search(SessionMgr.getSubjectKey(), query, mapToEntities);
	}
        
	public SolrResults searchSolr(SolrQuery query) throws Exception {
		return EJBFactory.getRemoteSolrBean().search(SessionMgr.getSubjectKey(), query, true);
	}

	public void updateIndex(DomainObject domainObj) throws Exception {
		EJBFactory.getRemoteSolrBean().updateIndex(domainObj);
	}

	public void removeFromIndex(Long domainObjId) throws Exception {
		EJBFactory.getRemoteSolrBean().removeFromIndex(domainObjId);
	}

	public void addAncestorToIndex(Long domainObjId, Long ancestorId) throws Exception {
		EJBFactory.getRemoteSolrBean().addAncestorToIndex(domainObjId, ancestorId);
	}
	
	public Map<String, SageTerm> getImageVocabulary() throws Exception {
		return EJBFactory.getRemoteSolrBean().getImageVocabulary();
	}
	
}
