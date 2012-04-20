package org.janelia.it.FlyWorkstation.api.facade.concrete_facade.aggregate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.EntityFacade;
import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.OntologyFacade;
import org.janelia.it.FlyWorkstation.api.stub.data.DuplicateDataException;
import org.janelia.it.FlyWorkstation.api.stub.data.NoDataException;
import org.janelia.it.jacs.model.entity.*;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/22/11
 * Time: 4:50 PM
 */
public class AggregateEntityFacade extends AggregateFacadeBase implements EntityFacade {

    // todo This needs to be cleaned up.  This type will get ignored
    static private Object[] parameters = new Object[]{EntityConstants.TYPE_FOLDER};

    protected String getMethodNameForAggregates() {
        return ("getFacade");
    }

    protected Class[] getParameterTypesForAggregates() {
        return new Class[]{String.class};
    }

    protected Object[] getParametersForAggregates() {
        return parameters;
    }

    @Override
    public List<EntityType> getEntityTypes() {
        Object[] aggregates = getAggregates();
        List<EntityType> returnList = new ArrayList<EntityType>();
        List<EntityType> tmpList;
        for (Object aggregate : aggregates) {
            tmpList = ((EntityFacade) aggregate).getEntityTypes();
            if (null != tmpList) {
                returnList.addAll(tmpList);
            }
        }
        return returnList;
    }

    @Override
    public List<EntityAttribute> getEntityAttributes() {
    	throw new UnsupportedOperationException();
    }

    @Override
    public Entity getEntityById(String entityId) throws Exception {
        Object[] aggregates = getAggregates();
        List<Entity> returnList = new ArrayList<Entity>();
        Entity tmpEntity;
        for (Object aggregate : aggregates) {
            tmpEntity = ((EntityFacade) aggregate).getEntityById(entityId);
            if (null != tmpEntity) {
                returnList.add(tmpEntity);
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

    @Override
    public List<Entity> getEntitiesById(List<Long> entityIds) throws Exception {
    	throw new UnsupportedOperationException();
    }
    
    @Override
    public Entity getEntityTree(Long entityId) throws Exception {
        Object[] aggregates = getAggregates();
        List<Entity> returnList = new ArrayList<Entity>();
        Entity tmpEntity;
        for (Object aggregate : aggregates) {
            tmpEntity = ((EntityFacade) aggregate).getEntityTree(entityId);
            if (null != tmpEntity) {
                returnList.add(tmpEntity);
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

    @Override
    public Entity getCachedEntityTree(Long entityId) throws Exception {
        Object[] aggregates = getAggregates();
        List<Entity> returnList = new ArrayList<Entity>();
        Entity tmpEntity;
        for (Object aggregate : aggregates) {
            tmpEntity = ((EntityFacade) aggregate).getCachedEntityTree(entityId);
            if (null != tmpEntity) {
                returnList.add(tmpEntity);
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

    @Override
    public List<Entity> getEntitiesByName(String entityName) {
        Object[] aggregates = getAggregates();
        List<Entity> returnList = new ArrayList<Entity>();
        List<Entity> tmpEntities;
        for (Object aggregate : aggregates) {
            tmpEntities = ((EntityFacade) aggregate).getEntitiesByName(entityName);
            if (null != tmpEntities) {
                returnList.addAll(tmpEntities);
            }
        }
        return returnList;
    }

    @Override
    public List<Entity> getUserCommonRootEntitiesByTypeName(String entityTypeName) {
        Object[] aggregates = getAggregates();
        List<Entity> returnList = new ArrayList<Entity>();
        List<Entity> tmpEntities;
        for (Object aggregate : aggregates) {
            tmpEntities = ((EntityFacade) aggregate).getUserCommonRootEntitiesByTypeName(entityTypeName);
            if (null != tmpEntities) {
                returnList.addAll(tmpEntities);
            }
        }
        return returnList;
    }

    @Override
    public List<Entity> getSystemCommonRootEntitiesByTypeName(String entityTypeName) {
        Object[] aggregates = getAggregates();
        List<Entity> returnList = new ArrayList<Entity>();
        List<Entity> tmpEntities;
        for (Object aggregate : aggregates) {
            tmpEntities = ((EntityFacade) aggregate).getSystemCommonRootEntitiesByTypeName(entityTypeName);
            if (null != tmpEntities) {
                returnList.addAll(tmpEntities);
            }
        }
        return returnList;
    }

    @Override
    public List<EntityData> getParentEntityDatas(Long childEntityId) {
        Object[] aggregates = getAggregates();
        List<EntityData> returnList = new ArrayList<EntityData>();
        List<EntityData> tmpEntityData;
        for (Object aggregate : aggregates) {
            tmpEntityData = ((EntityFacade) aggregate).getParentEntityDatas(childEntityId);
            if (null != tmpEntityData) {
                returnList.addAll(tmpEntityData);
            }
        }
        return returnList;
    }
    
    @Override
    public List<Entity> getParentEntities(Long childEntityId) {
        Object[] aggregates = getAggregates();
        List<Entity> returnList = new ArrayList<Entity>();
        List<Entity> tmpEntityData;
        for (Object aggregate : aggregates) {
            tmpEntityData = ((EntityFacade) aggregate).getParentEntities(childEntityId);
            if (null != tmpEntityData) {
                returnList.addAll(tmpEntityData);
            }
        }
        return returnList;
    }

    @Override
    public Set<Entity> getChildEntities(Long parentEntityId) {
        Object[] aggregates = getAggregates();
        Set<Entity> returnSet = new HashSet<Entity>();
        Set<Entity> tmpEntitySet;
        for (Object aggregate : aggregates) {
            tmpEntitySet = ((EntityFacade) aggregate).getChildEntities(parentEntityId);
            if (null != tmpEntitySet) {
                returnSet.addAll(tmpEntitySet);
            }
        }
        return returnSet;
    }

    @Override
    public Entity cloneEntityTree(Long entityId, String rootName) throws Exception {
        Object[] aggregates = getAggregates();
        List<Entity> returnList = new ArrayList<Entity>();
        Entity tmpEntity;
        for (Object aggregate : aggregates) {
            tmpEntity = ((EntityFacade) aggregate).cloneEntityTree(entityId, rootName);
            if (null != tmpEntity) {
                returnList.add(tmpEntity);
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

    @Override
    public List<Entity> getEntitiesByTypeName(String entityTypeName) {
        Object[] aggregates = getAggregates();
        List<Entity> returnList = new ArrayList<Entity>();
        List<Entity> tmpEntityData;
        for (Object aggregate : aggregates) {
            tmpEntityData = ((EntityFacade) aggregate).getEntitiesByTypeName(entityTypeName);
            if (null != tmpEntityData) {
                returnList.addAll(tmpEntityData);
            }
        }
        return returnList;
    }

    @Override
    public boolean deleteEntityById(Long entityId) {
        Object[] aggregates = getAggregates();
        List<Boolean> returnList = new ArrayList<Boolean>();
        for (Object aggregate : aggregates) {
            returnList.add(((EntityFacade) aggregate).deleteEntityById(entityId));
        }
        boolean returnSuccess = false;
        for (Boolean aBoolean : returnList) {
            if (aBoolean) {
                returnSuccess = aBoolean;
                break;
            }
        }
        // Initially, we're assuming that only one data source has an entity
        return returnSuccess;
    }

    @Override
    public void deleteEntityTree(Long entityId) throws Exception {
        Object[] aggregates = getAggregates();
        for (Object aggregate : aggregates) {
            ((EntityFacade) aggregate).deleteEntityTree(entityId);
        }
    }

    @Override
    public Entity saveEntity(Entity entity) throws Exception {
        Object[] aggregates = getAggregates();
        List<Entity> returnList = new ArrayList<Entity>();
        Entity tmpEntity;
        for (Object aggregate : aggregates) {
        	tmpEntity = ((OntologyFacade) aggregate).saveEntity(entity);
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
    
    @Override
    public EntityData saveEntityDataForEntity(EntityData newData) throws Exception {
        Object[] aggregates = getAggregates();
        List<EntityData> returnList = new ArrayList<EntityData>();
        EntityData tmpEntityData;
        for (Object aggregate : aggregates) {
            tmpEntityData = ((OntologyFacade) aggregate).saveEntityDataForEntity(newData);
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

    @Override
    public Entity createEntity(String entityTypeName, String entityName) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntityData addEntityToParent(Entity parent, Entity entity, Integer index, String attrName) throws Exception {
    	throw new UnsupportedOperationException();
    }

    @Override
    public void removeEntityData(EntityData ed) throws Exception{
    	throw new UnsupportedOperationException();
    }
}
