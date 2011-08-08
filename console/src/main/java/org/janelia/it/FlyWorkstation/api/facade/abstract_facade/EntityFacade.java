package org.janelia.it.FlyWorkstation.api.facade.abstract_facade;

import org.janelia.it.FlyWorkstation.api.stub.data.DuplicateDataException;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/25/11
 * Time: 3:52 PM
 */
public interface EntityFacade {
    public List<EntityType> getEntityTypes();

    public Entity getEntityById(String entityId) throws Exception;

    public Entity getEntityTree(Long entityId) throws DuplicateDataException;

    public Entity getCachedEntityTree(Long entityId) throws DuplicateDataException;

    public List<Entity> getEntitiesByName(String entityName);

    public List<Entity> getCommonRootEntitiesByType(Long entityTypeId);

    public Set<Entity> getChildEntities(Long parentEntityId);

    public List<EntityData> getParentEntityDatas(Long childEntityId);

    public List<Entity> getEntitiesByType(Long entityTypeId);

    public EntityData saveEntityDataForEntity(EntityData newData) throws Exception;

    public boolean deleteEntityById(Long entityId);

    public void deleteEntityTree(String userLogin, Long entityId) throws Exception;

    public Entity cloneEntityTree(Long entityId, String username, String rootName) throws Exception;
}
