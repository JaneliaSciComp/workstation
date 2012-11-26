package org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.EntityFacade;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.compute.api.support.MappedId;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 8/5/11
 * Time: 10:49 AM
 */
public class EJBEntityFacade implements EntityFacade {
    @Override
    public List<EntityType> getEntityTypes() {
        return EJBFactory.getRemoteEntityBean().getEntityTypes();
    }
    
    @Override
    public List<EntityAttribute> getEntityAttributes() {
    	return EJBFactory.getRemoteEntityBean().getEntityAttributes();
    }
    
    @Override
    public Entity getEntityById(String entityId) {
        return EJBFactory.getRemoteEntityBean().getEntityById(entityId);
    }
    
    @Override
    public List<Entity> getEntitiesById(List<Long> entityIds) throws Exception {
        return EJBFactory.getRemoteEntityBean().getEntitiesById(entityIds);
    }
    
    @Override
    public Entity getEntityTree(Long entityId) {
        return EJBFactory.getRemoteEntityBean().getEntityTree(entityId);
    }

    @Override
    public Entity getEntityAndChildren(Long entityId) {
        return EJBFactory.getRemoteEntityBean().getEntityAndChildren(entityId);
    }

    @Override
    public ArrayList<Entity> getEntitiesByName(String entityName) {
        return new ArrayList<Entity>(EJBFactory.getRemoteEntityBean().getEntitiesByName(entityName));
    }

    @Override
    public List<Entity> getUserCommonRootEntitiesByTypeName(String entityTypeName) {
        return EJBFactory.getRemoteAnnotationBean().getCommonRootEntitiesByTypeName(SessionMgr.getUsername(), entityTypeName);
    }

    @Override
    public List<Entity> getSystemCommonRootEntitiesByTypeName(String entityTypeName) {
        return EJBFactory.getRemoteAnnotationBean().getCommonRootEntitiesByTypeName("system", entityTypeName);
    }

    @Override
    public List<List<EntityData>> getPathsToRoots(Long entityId) throws Exception {
    	Entity entity = EJBFactory.getRemoteEntityBean().getEntityById(entityId+"");
    	return EJBFactory.getRemoteEntityBean().getPathsToRoots(SessionMgr.getUsername(), entity);
    }
    
    @Override
    public List<EntityData> getParentEntityDatas(Long childEntityId) {
    	List<EntityData> list = new ArrayList<EntityData>();
    	Set<EntityData> set = EJBFactory.getRemoteEntityBean().getParentEntityDatas(childEntityId);
    	if (set==null) return list;
    	list.addAll(set);
        return list;
    }
    
    @Override
    public Set<Long> getParentIdsForAttribute(long childEntityId, String attributeName) {
    	Set<Long> set = new HashSet<Long>();
    	Set<Long> results = EJBFactory.getRemoteEntityBean().getParentIdsForAttribute(childEntityId, attributeName);
    	if (results==null) return set;
    	set.addAll(results);
        return set;
    }
    
    @Override
    public List<Entity> getParentEntities(Long childEntityId) {
    	List<Entity> list = new ArrayList<Entity>();
    	Set<Entity> set = EJBFactory.getRemoteEntityBean().getParentEntities(childEntityId);
    	if (set==null) return list;
    	list.addAll(set);
        return list;
    }
    
    @Override
    public Set<Entity> getChildEntities(Long parentEntityId) {
        return EJBFactory.getRemoteEntityBean().getChildEntities(parentEntityId);
    }

    @Override
    public List<Entity> getEntitiesByTypeName(String entityTypeName) {
        return EJBFactory.getRemoteEntityBean().getEntitiesByTypeName(entityTypeName);
    }

    @Override
    public Entity saveEntity(Entity entity) throws Exception {
        return EJBFactory.getRemoteEntityBean().saveOrUpdateEntity(SessionMgr.getUsername(), entity);
    }
    
    @Override
    public EntityData saveEntityDataForEntity(EntityData newData) throws Exception {
        return EJBFactory.getRemoteEntityBean().saveOrUpdateEntityData(SessionMgr.getUsername(), newData);
    }

    @Override
    public boolean deleteEntityById(Long entityId) throws Exception {
        return EJBFactory.getRemoteEntityBean().deleteEntityById(SessionMgr.getUsername(), entityId);
    }

    @Override
    public void deleteEntityTree(Long entityId) throws Exception {
        EJBFactory.getRemoteEntityBean().deleteEntityTree(SessionMgr.getUsername(), entityId);
    }
    
    @Override
    public void deleteEntityTree(Long entityId, boolean unlinkMultipleParents) throws Exception {
	    EJBFactory.getRemoteEntityBean().deleteSmallEntityTree(SessionMgr.getUsername(), entityId, unlinkMultipleParents);
    }

    @Override
    public Entity cloneEntityTree(Long entityId, String rootName) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().cloneEntityTree(SessionMgr.getUsername(), entityId, rootName);
    }
    
    @Override
    public Entity createEntity(String entityTypeName, String entityName) throws Exception {
        return EJBFactory.getRemoteEntityBean().createEntity(SessionMgr.getUsername(), entityTypeName, entityName);
    }

    @Override
    public EntityData addEntityToParent(Entity parent, Entity entity, Integer index, String attrName) throws Exception {
        return EJBFactory.getRemoteEntityBean().addEntityToParent(SessionMgr.getUsername(), parent, entity, index, attrName);
    }
    
    @Override
    public void removeEntityData(EntityData ed) throws Exception {
        EJBFactory.getRemoteEntityBean().deleteEntityData(SessionMgr.getUsername(), ed);
    }

    public void createEntityType(String typeName) throws Exception {
    	EJBFactory.getRemoteEntityBean().createNewEntityType(typeName);
    }
    
    public void createEntityAttribute(String typeName, String attrName) throws Exception {
    	EJBFactory.getRemoteEntityBean().createNewEntityAttr(typeName, attrName);
    }
    
    public Entity getAncestorWithType(Entity entity, String typeName) throws Exception {
    	return EJBFactory.getRemoteEntityBean().getAncestorWithType(entity, typeName);
    }

	public void addChildren(Long parentId, List<Long> childrenIds, String attributeName) throws Exception {
    	EJBFactory.getRemoteEntityBean().addChildren(SessionMgr.getUsername(), parentId, childrenIds, attributeName);
	}
	
	public List<MappedId> getProjectedResults(List<Long> entityIds, List<String> upMapping, List<String> downMapping) throws Exception {
		return EJBFactory.getRemoteEntityBean().getProjectedResults(entityIds, upMapping, downMapping);
	}
}
