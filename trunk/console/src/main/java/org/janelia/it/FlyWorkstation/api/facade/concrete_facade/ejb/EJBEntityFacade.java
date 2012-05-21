package org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.EntityFacade;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
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
    public List<EntityData> getParentEntityDatas(Long childEntityId) {
        return new ArrayList<EntityData>(EJBFactory.getRemoteEntityBean().getParentEntityDatas(childEntityId));
    }

    @Override
    public List<Entity> getParentEntities(Long childEntityId) {
        return new ArrayList<Entity>(EJBFactory.getRemoteEntityBean().getParentEntities(childEntityId));
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
}
