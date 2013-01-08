package org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.EntityFacade;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.compute.api.support.MappedId;
import org.janelia.it.jacs.model.entity.*;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 8/5/11
 * Time: 10:49 AM
 */
public class EJBEntityFacade implements EntityFacade {
    @Override
    public List<EntityType> getEntityTypes() throws Exception {
        return EJBFactory.getRemoteEntityBean().getEntityTypes();
    }
    
    @Override
    public List<EntityAttribute> getEntityAttributes() throws Exception {
    	return EJBFactory.getRemoteEntityBean().getEntityAttributes();
    }
    
    @Override
    public Entity getEntityById(Long entityId) throws Exception {
        return EJBFactory.getRemoteEntityBean().getEntityById(SessionMgr.getSubjectKey(), entityId);
    }
    
    @Override
    public List<Entity> getEntitiesById(List<Long> entityIds) throws Exception {
        return EJBFactory.getRemoteEntityBean().getEntitiesById(SessionMgr.getSubjectKey(), entityIds);
    }
    
    @Override
    public Entity getEntityTree(Long entityId) throws Exception {
        return EJBFactory.getRemoteEntityBean().getEntityTree(SessionMgr.getSubjectKey(), entityId);
    }

    @Override
    public Entity getEntityAndChildren(Long entityId) throws Exception {
        return EJBFactory.getRemoteEntityBean().getEntityAndChildren(SessionMgr.getSubjectKey(), entityId);
    }

    @Override
    public ArrayList<Entity> getEntitiesByName(String entityName) throws Exception {
        return new ArrayList<Entity>(EJBFactory.getRemoteEntityBean().getUserEntitiesByName(SessionMgr.getSubjectKey(), entityName));
    }

    @Override
    public List<Entity> getCommonRootEntities() throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getCommonRootEntities(SessionMgr.getSubjectKey());
    }

    @Override
    public List<List<EntityData>> getPathsToRoots(Long entityId) throws Exception {
    	Entity entity = EJBFactory.getRemoteEntityBean().getEntityById(SessionMgr.getSubjectKey(), entityId);
    	return EJBFactory.getRemoteEntityBean().getPathsToRoots(SessionMgr.getSubjectKey(), entity.getId());
    }
    
    @Override
    public List<EntityData> getParentEntityDatas(Long childEntityId) throws Exception {
    	List<EntityData> list = new ArrayList<EntityData>();
    	Set<EntityData> set = EJBFactory.getRemoteEntityBean().getParentEntityDatas(SessionMgr.getSubjectKey(), childEntityId);
    	if (set==null) return list;
    	list.addAll(set);
        return list;
    }
    
    @Override
    public Set<Long> getParentIdsForAttribute(long childEntityId, String attributeName) throws Exception {
    	Set<Long> set = new HashSet<Long>();
    	Set<Long> results = EJBFactory.getRemoteEntityBean().getParentIdsForAttribute(SessionMgr.getSubjectKey(), childEntityId, attributeName);
    	if (results==null) return set;
    	set.addAll(results);
        return set;
    }
    
    @Override
    public List<Entity> getParentEntities(Long childEntityId) throws Exception {
    	List<Entity> list = new ArrayList<Entity>();
    	Set<Entity> set = EJBFactory.getRemoteEntityBean().getParentEntities(SessionMgr.getSubjectKey(), childEntityId);
    	if (set==null) return list;
    	list.addAll(set);
        return list;
    }
    
    @Override
    public Set<Entity> getChildEntities(Long parentEntityId) throws Exception {
        return EJBFactory.getRemoteEntityBean().getChildEntities(SessionMgr.getSubjectKey(), parentEntityId);
    }

    @Override
    public List<Entity> getEntitiesByTypeName(String entityTypeName) throws Exception {
        return EJBFactory.getRemoteEntityBean().getUserEntitiesByTypeName(SessionMgr.getSubjectKey(), entityTypeName);
    }

    @Override
    public Entity saveEntity(Entity entity) throws Exception {
        return EJBFactory.getRemoteEntityBean().saveOrUpdateEntity(SessionMgr.getSubjectKey(), entity);
    }
    
    @Override
    public EntityData saveEntityDataForEntity(EntityData newData) throws Exception {
        return EJBFactory.getRemoteEntityBean().saveOrUpdateEntityData(SessionMgr.getSubjectKey(), newData);
    }

    @Override
    public boolean deleteEntityById(Long entityId) throws Exception {
        return EJBFactory.getRemoteEntityBean().deleteEntityById(SessionMgr.getSubjectKey(), entityId);
    }

    @Override
    public void deleteEntityTree(Long entityId) throws Exception {
        EJBFactory.getRemoteEntityBean().deleteEntityTree(SessionMgr.getSubjectKey(), entityId);
    }
    
    @Override
    public void deleteEntityTree(Long entityId, boolean unlinkMultipleParents) throws Exception {
	    EJBFactory.getRemoteEntityBean().deleteSmallEntityTree(SessionMgr.getSubjectKey(), entityId, unlinkMultipleParents);
    }

    @Override
    public Entity cloneEntityTree(Long entityId, String rootName) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().cloneEntityTree(SessionMgr.getSubjectKey(), entityId, rootName);
    }
    
    @Override
    public Entity createEntity(String entityTypeName, String entityName) throws Exception {
        return EJBFactory.getRemoteEntityBean().createEntity(SessionMgr.getSubjectKey(), entityTypeName, entityName);
    }

    @Override
    public EntityData addEntityToParent(Entity parent, Entity entity, Integer index, String attrName) throws Exception {
        return EJBFactory.getRemoteEntityBean().addEntityToParent(SessionMgr.getSubjectKey(), parent.getId(), entity.getId(), index, attrName);
    }
    
    @Override
    public void removeEntityData(EntityData ed) throws Exception {
        EJBFactory.getRemoteEntityBean().deleteEntityData(SessionMgr.getSubjectKey(), ed.getId());
    }

    public void createEntityType(String typeName) throws Exception {
    	EJBFactory.getRemoteEntityBean().createNewEntityType(typeName);
    }
    
    public void createEntityAttribute(String typeName, String attrName) throws Exception {
    	EJBFactory.getRemoteEntityBean().createNewEntityAttr(typeName, attrName);
    }
    
    public Entity getAncestorWithType(Entity entity, String typeName) throws Exception {
    	return EJBFactory.getRemoteEntityBean().getAncestorWithType(SessionMgr.getSubjectKey(), entity.getId(), typeName);
    }

	public void addChildren(Long parentId, List<Long> childrenIds, String attributeName) throws Exception {
    	EJBFactory.getRemoteEntityBean().addChildren(SessionMgr.getSubjectKey(), parentId, childrenIds, attributeName);
	}
	
	public List<MappedId> getProjectedResults(List<Long> entityIds, List<String> upMapping, List<String> downMapping) throws Exception {
		return EJBFactory.getRemoteEntityBean().getProjectedResults(SessionMgr.getSubjectKey(), entityIds, upMapping, downMapping);
	}

	public Set<EntityActorPermission> getFullPermissions(Long entityId) throws Exception {
	    return EJBFactory.getRemoteEntityBean().getFullPermissions(SessionMgr.getSubjectKey(), entityId);
	}
	
    @Override
    public EntityActorPermission grantPermissions(Long entityId, String granteeKey, String permissions, boolean recursive) throws Exception {
    	return EJBFactory.getRemoteEntityBean().grantPermissions(SessionMgr.getSubjectKey(), entityId, granteeKey, permissions, recursive);
    }

    @Override
    public void revokePermissions(Long entityId, String revokeeKey, boolean recursive) throws Exception {
    	EJBFactory.getRemoteEntityBean().revokePermissions(SessionMgr.getSubjectKey(), entityId, revokeeKey, recursive);
    }
    
    @Override
    public EntityActorPermission saveOrUpdatePermission(EntityActorPermission eap) throws Exception {
    	return EJBFactory.getRemoteEntityBean().saveOrUpdatePermission(SessionMgr.getSubjectKey(), eap);
    }
}
