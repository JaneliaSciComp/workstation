package org.janelia.it.FlyWorkstation.api.facade.concrete_facade.aggregate;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/22/11
 * Time: 4:58 PM
 */

import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.*;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManagerBase;
import org.janelia.it.jacs.model.entity.EntityConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class provides the concrete implementation of FacadeManagerBase for the Aggregate Protocol
 */
public class AggregateFacadeManager extends FacadeManagerBase {

    private AggregateOntologyFacade ontologyFacade;
    private AggregateEntityFacade entityFacade;
    private AggregateAnnotationFacade annotationFacade;
    private AggregateSolrFacade solrFacade;
    private AggregateComputeFacade computeFacade;
    private AggregateControlledVocabService vocabService;

    public AggregateFacadeManager() {
    }

    public String getDataSourceSelectorClass() {
        return "";
    }

    public Object[] getOpenDataSources() {
        List inUseProtocols = FacadeManager.getInUseProtocolStrings();
        String[] inUseProtocolArray = new String[inUseProtocols.size()];
        inUseProtocols.toArray(inUseProtocolArray);
        List dataSources = new ArrayList();
        Object[] tmpArray;
        for (String anInUseProtocolArray : inUseProtocolArray) {
            tmpArray = FacadeManager.getFacadeManager(anInUseProtocolArray).getOpenDataSources();
            dataSources.addAll(Arrays.asList(tmpArray));
        }
        return dataSources.toArray();
    }

    public String getServerName() {
        return "";
    }

    public EntityFacade getFacade(String entityTypeName) throws Exception {
        //Facades listed explicitly have some methods and therefore need to be coded
        //All others simply inherit Feature Facade and therefore can use the comman Interface
        if (EntityConstants.TYPE_ONTOLOGY_ROOT.equals(entityTypeName) || EntityConstants.TYPE_ONTOLOGY_ELEMENT.equals(entityTypeName)) {
            return getOntologyFacade();
        }
        else {
            return getEntityFacade();
        }
    }

    public ControlledVocabService getControlledVocabService() throws Exception {
        if (vocabService == null) vocabService = new AggregateControlledVocabService();
        return vocabService;
    }

    public OntologyFacade getOntologyFacade() {
        if (null == ontologyFacade) {
            ontologyFacade = new AggregateOntologyFacade();
        }
        return ontologyFacade;
    }

    public EntityFacade getEntityFacade() {
        if (null == entityFacade) {
            entityFacade = new AggregateEntityFacade();
        }
        return entityFacade;
    }

    @Override
    public AnnotationFacade getAnnotationFacade() {
        if (null == annotationFacade) {
            annotationFacade = new AggregateAnnotationFacade();
        }
        return annotationFacade;
    }

    @Override
    public SolrFacade getSolrFacade() {
        if (null == solrFacade) {
        	solrFacade = new AggregateSolrFacade();
        }
        return solrFacade;
    }
    
    @Override
    public ComputeFacade getComputeFacade() {
        if (null == computeFacade) {
            computeFacade = new AggregateComputeFacade();
        }
        return computeFacade;
    }

}
