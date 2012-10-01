package org.janelia.it.FlyWorkstation.api.facade.concrete_facade.aggregate;

import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.OntologyFacade;
import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.SearchFacade;
import org.janelia.it.FlyWorkstation.api.stub.data.DuplicateDataException;
import org.janelia.it.FlyWorkstation.api.stub.data.NoDataException;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/22/11
 * Time: 4:50 PM
 */
public class AggregateSearchFacade extends AggregateFacadeBase implements SearchFacade {

    protected String getMethodNameForAggregates() {
        return "getFacade";
    }

    protected Class[] getParameterTypesForAggregates() {
        return new Class[0];
    }

    protected Object[] getParametersForAggregates() {
        return new Object[0];
    }


    public Long getOntology() {
        Object[] aggregates = getAggregates();
        List<Entity> returnList = new ArrayList<Entity>();
        List<Entity> tmpList;
        for (Object aggregate : aggregates) {
            tmpList = ((OntologyFacade) aggregate).getOntologies();
            if (tmpList != null) {
                returnList.addAll(tmpList);
            }
        }
        return 0l;
//        return returnList;
    }

    public Entity createOntologyAnnotation(OntologyAnnotation annotation) throws Exception {
        Object[] aggregates = getAggregates();
        List<Entity> returnList = new ArrayList<Entity>();
        Entity tmpEntity;
        for (Object aggregate : aggregates) {
            tmpEntity = ((OntologyFacade) aggregate).createOntologyAnnotation(annotation);
            if (tmpEntity != null) {
                returnList.add(tmpEntity);
            }
        }
        // Only one facade should be allowing saves of data; therefore, only one item should be returned
        if (1 < returnList.size()) {
            throw new DuplicateDataException();
        }
        if (1 == returnList.size()) {
            return returnList.get(0);
        }
        throw new NoDataException();
    }

    public void removeOntologyAnnotation(Long annotationId) throws Exception {
        for (Object aggregate : getAggregates()) {
            ((OntologyFacade) aggregate).removeOntologyAnnotation(annotationId);
        }
    }

    public Entity createOntologyRoot(String ontologyName) throws Exception {
        Object[] aggregates = getAggregates();
        List<Entity> returnList = new ArrayList<Entity>();
        Entity tmpEntity;
        for (Object aggregate : aggregates) {
            tmpEntity = ((OntologyFacade) aggregate).createOntologyRoot(ontologyName);
            if (tmpEntity != null) {
                returnList.add(tmpEntity);
            }
        }
        // Only one facade should be allowing saves of data; therefore, only one item should be returned
        if (1 < returnList.size()) {
            throw new DuplicateDataException();
        }
        if (1 == returnList.size()) {
            return returnList.get(0);
        }
        throw new NoDataException();
    }

    public EntityData createOntologyTerm(Long parentEntityId, String label, OntologyElementType type, Integer orderIndex) throws Exception {
        Object[] aggregates = getAggregates();
        List<EntityData> returnList = new ArrayList<EntityData>();
        EntityData tmpEntityData;
        for (Object aggregate : aggregates) {
            tmpEntityData = ((OntologyFacade) aggregate).createOntologyTerm(parentEntityId, label, type, orderIndex);
            if (tmpEntityData != null) {
                returnList.add(tmpEntityData);
            }
        }
        // Only one facade should be allowing saves of data; therefore, only one item should be returned
        if (1 < returnList.size()) {
            throw new DuplicateDataException();
        }
        if (1 == returnList.size()) {
            return returnList.get(0);
        }
        throw new NoDataException();
    }

    public Entity getOntologyTree(Long rootEntityId) throws Exception {
        Object[] aggregates = getAggregates();
        List<Entity> returnList = new ArrayList<Entity>();
        Entity tmpEntityData;
        for (Object aggregate : aggregates) {
            tmpEntityData = ((OntologyFacade) aggregate).getOntologyTree(rootEntityId);
            if (tmpEntityData != null) {
                returnList.add(tmpEntityData);
            }
        }
        if (1 < returnList.size()) {
            throw new DuplicateDataException();
        }
        if (1 == returnList.size()) {
            return returnList.get(0);
        }
        return null;
    }

    public List<Entity> getPrivateOntologies() throws Exception {
        Object[] aggregates = getAggregates();
        List<Entity> returnList = new ArrayList<Entity>();
        List<Entity> tmpList;
        for (Object aggregate : aggregates) {
            tmpList = ((OntologyFacade) aggregate).getPrivateOntologies();
            if (tmpList != null) {
                returnList.addAll(tmpList);
            }
        }
        return returnList;
    }

    public List<Entity> getPublicOntologies() throws Exception {
        Object[] aggregates = getAggregates();
        List<Entity> returnList = new ArrayList<Entity>();
        List<Entity> tmpList;
        for (Object aggregate : aggregates) {
            tmpList = ((OntologyFacade) aggregate).getPublicOntologies();
            if (tmpList != null) {
                returnList.addAll(tmpList);
            }
        }
        return returnList;
    }

    public Entity publishOntology(Long ontologyEntityId, String rootName) throws Exception {
        Object[] aggregates = getAggregates();
        List<Entity> returnList = new ArrayList<Entity>();
        Entity tmpEntity;
        for (Object aggregate : aggregates) {
            tmpEntity = ((OntologyFacade) aggregate).publishOntology(ontologyEntityId, rootName);
            if (tmpEntity != null) {
                returnList.add(tmpEntity);
            }
        }
        // Only one facade should be allowing saves of data; therefore, only one item should be returned
        if (1 < returnList.size()) {
            throw new DuplicateDataException();
        }
        if (1 == returnList.size()) {
            return returnList.get(0);
        }
        throw new NoDataException();
    }

    public void removeOntologyTerm(Long termEntityId) throws Exception {
    }

    public void submitSearch(Long termEntityId) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
