package org.janelia.it.FlyWorkstation.api.facade.abstract_facade;

import java.util.List;
import java.util.Set;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/25/11
 * Time: 3:52 PM
 */
public interface EntityFacade {
	
    public List<EntityType> getEntityTypes();
    
    public List<EntityAttribute> getEntityAttributes();
    
    public List<Entity> getEntitiesById(List<Long> entityIds) throws Exception;
    
    public Entity getEntityById(String entityId) throws Exception;

    public Entity getEntityTree(Long entityId) throws Exception;

    public Entity getCachedEntityTree(Long entityId) throws Exception;

    public List<Entity> getEntitiesByName(String entityName);

    public List<Entity> getUserCommonRootEntitiesByTypeName(String entityTypeName);

    public List<Entity> getSystemCommonRootEntitiesByTypeName(String entityTypeName);
    
    public Set<Entity> getChildEntities(Long parentEntityId);

    public List<EntityData> getParentEntityDatas(Long childEntityId);
    
    public List<Entity> getParentEntities(Long childEntityId);

    public List<Entity> getEntitiesByTypeName(String entityTypeName);

    public Entity saveEntity(Entity entity) throws Exception;
    
    public EntityData saveEntityDataForEntity(EntityData newData) throws Exception;
    
    public boolean deleteEntityById(Long entityId);

    public void deleteEntityTree(Long entityId) throws Exception;

    public Entity cloneEntityTree(Long entityId, String rootName) throws Exception;

    public Entity createEntity(String entityTypeName, String entityName) throws Exception;
    
    public EntityData addEntityToParent(Entity parent, Entity entity, Integer index, String attrName) throws Exception;
    
    public void removeEntityData(EntityData ed) throws Exception;
}
