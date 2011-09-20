package org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb;

import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.EntityFacade;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 8/5/11
 * Time: 10:49 AM
 */
public class EJBEntityFacade implements EntityFacade {
    @Override
    public List<EntityType> getEntityTypes() {
        return EJBFactory.getRemoteAnnotationBean().getEntityTypes();
    }

    @Override
    public Entity getEntityById(String entityId) {
        return EJBFactory.getRemoteAnnotationBean().getEntityById(entityId);
    }

    @Override
    public Entity getEntityTree(Long entityId) {
        return EJBFactory.getRemoteAnnotationBean().getEntityTree(entityId);
    }

    @Override
    public Entity getCachedEntityTree(Long entityId) {
        return EJBFactory.getRemoteAnnotationBean().getCachedEntityTree(entityId);
    }

    @Override
    public ArrayList<Entity> getEntitiesByName(String entityName) {
        return new ArrayList<Entity>(EJBFactory.getRemoteAnnotationBean().getEntitiesByName(entityName));
    }

    @Override
    public List<Entity> getCommonRootEntitiesByTypeName(String entityTypeName) {
        return EJBFactory.getRemoteAnnotationBean().getCommonRootEntitiesByTypeName(entityTypeName);
    }

    @Override
    public List<EntityData> getParentEntityDatas(Long childEntityId) {
        return new ArrayList<EntityData>(EJBFactory.getRemoteAnnotationBean().getParentEntityDatas(childEntityId));
    }

    @Override
    public Set<Entity> getChildEntities(Long parentEntityId) {
        return EJBFactory.getRemoteAnnotationBean().getChildEntities(parentEntityId);
    }

    @Override
    public List<Entity> getEntitiesByTypeName(String entityTypeName) {
        return EJBFactory.getRemoteAnnotationBean().getEntitiesByTypeName(entityTypeName);
    }

    @Override
    public Entity saveEntity(Entity entity) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().saveOrUpdateEntity(entity);
    }
    
    @Override
    public EntityData saveEntityDataForEntity(EntityData newData) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().saveOrUpdateEntityData(newData);
    }

    @Override
    public boolean deleteEntityById(Long entityId) {
        return EJBFactory.getRemoteAnnotationBean().deleteEntityById(entityId);
    }

    @Override
    public void deleteEntityTree(Long entityId) throws Exception {
        EJBFactory.getRemoteAnnotationBean().deleteEntityTree(SessionMgr.getUsername(), entityId);
    }

    @Override
    public Entity cloneEntityTree(Long entityId, String rootName) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().cloneEntityTree(entityId, SessionMgr.getUsername()
                , rootName);
    }
}
