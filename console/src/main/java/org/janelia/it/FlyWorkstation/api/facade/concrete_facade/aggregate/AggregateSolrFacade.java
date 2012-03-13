package org.janelia.it.FlyWorkstation.api.facade.concrete_facade.aggregate;

import org.apache.solr.client.solrj.SolrQuery;
import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.SolrFacade;
import org.janelia.it.jacs.compute.api.support.SolrResults;

/**
 * Facade for running SOLR searches using the aggregate facade. Useless for now. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AggregateSolrFacade extends AggregateFacadeBase implements SolrFacade {

    protected String getMethodNameForAggregates() {
        return "getFacade";
    }

    protected Class[] getParameterTypesForAggregates() {
        return new Class[0];
    }

    protected Object[] getParametersForAggregates() {
        return new Object[0];
    }

	public SolrResults searchSolr(SolrQuery query) throws Exception {
		throw new UnsupportedOperationException();
	}

    
}
