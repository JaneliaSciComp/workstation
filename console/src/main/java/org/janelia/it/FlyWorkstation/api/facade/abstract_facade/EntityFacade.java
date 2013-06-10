package org.janelia.it.FlyWorkstation.api.facade.abstract_facade;

import org.janelia.it.jacs.compute.api.support.MappedId;
import org.janelia.it.jacs.model.entity.*;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/25/11
 * Time: 3:52 PM
 */
public interface EntityFacade {
	
    public List<EntityType> getEntityTypes() throws Exception;
    
    public List<EntityAttribute> getEntityAttributes() throws Exception;
    
    public List<Entity> getEntitiesById(List<Long> entityIds) throws Exception;
    
    public Entity getEntityById(Long entityId) throws Exception;

    public Entity getEntityAndChildren(Long entityId) throws Exception;

    public Entity getEntityTree(Long entityId) throws Exception;

    public List<Entity> getEntitiesByName(String entityName) throws Exception;
    
    public List<Entity> getOwnedEntitiesByName(String entityName) throws Exception;

    public List<Entity> getCommonRootEntities() throws Exception;
    
    public List<Entity> getAlignmentSpaces() throws Exception;
     
    public Set<Entity> getChildEntities(Long parentEntityId) throws Exception;

    public List<EntityData> getParentEntityDatas(Long childEntityId) throws Exception;
    
    public Set<Long> getParentIdsForAttribute(long childEntityId, String attributeName) throws Exception;
    
    public List<List<EntityData>> getPathsToRoots(Long entityId) throws Exception;
    
    public List<Entity> getParentEntities(Long childEntityId) throws Exception;

    public List<Entity> getEntitiesByTypeName(String entityTypeName) throws Exception;
    
    public List<Entity> getOwnedEntitiesByTypeName(String entityTypeName) throws Exception;

    public Entity saveEntity(Entity entity) throws Exception;
    
    public EntityData saveEntityDataForEntity(EntityData newData) throws Exception;
    
    public boolean deleteEntityById(Long entityId) throws Exception;

    public void deleteEntityTree(Long entityId) throws Exception;

    public void deleteEntityTree(Long entityId, boolean unlinkMultipleParents) throws Exception;
    
    public Entity cloneEntityTree(Long entityId, String rootName) throws Exception;

    public Entity createEntity(String entityTypeName, String entityName) throws Exception;
    
    public EntityData addEntityToParent(Entity parent, Entity entity, Integer index, String attrName) throws Exception;
    
    public void removeEntityData(EntityData ed) throws Exception;

    public void createEntityType(String typeName) throws Exception;
    
    public void createEntityAttribute(String typeName, String attrName) throws Exception;
    
    public Entity getAncestorWithType(Entity entity, String typeName) throws Exception;
    
	public List<MappedId> getProjectedResults(List<Long> entityIds, List<String> upMapping, List<String> downMapping) throws Exception;

	public void addChildren(Long parentId, List<Long> childrenIds, String attributeName) throws Exception;

	public Set<EntityActorPermission> getFullPermissions(Long entityId) throws Exception;
	
    public EntityActorPermission grantPermissions(Long entityId, String granteeKey, String permissions, boolean recursive) throws Exception;
    
    public void revokePermissions(Long entityId, String revokeeKey, boolean recursive) throws Exception;

	public EntityActorPermission saveOrUpdatePermission(EntityActorPermission eap) throws Exception;

    public TmWorkspace createTiledMicroscopeWorkspace(Long brainSampleId, String name, String ownerKey) throws Exception;

    public TmNeuron createTiledMicroscopeNeuron(Long workspaceId, String name) throws Exception;

    public TmGeoAnnotation addGeometricAnnotation(Long neuronId, Long parentAnnotationId, int index,
                                                  double x, double y, double z, String comment) throws Exception;

    public void updateGeometricAnnotation(TmGeoAnnotation geoAnnotation,
                                          int index, double x, double y, double z, String comment) throws Exception;

    public List<TmWorkspaceDescriptor> getWorkspacesForBrainSample(Long brainSampleId, String ownerKey) throws Exception;

    public List<TmNeuronDescriptor> getNeuronsForWorkspace(Long workspaceId, String ownerKey) throws Exception;

    public void removeWorkspacePreference(Long workspaceId, String key) throws Exception;

    public void createOrUpdateWorkspacePreference(Long workspaceId, String key, String value) throws Exception;

    public void deleteNeuron(String ownerKey, Long neuronId) throws Exception;

    public void deleteWorkspace(String ownerKey, Long workspaceId) throws Exception;

    public void deleteGeometricAnnotation(Long geoId) throws Exception;

    public TmWorkspace loadWorkspace(Long workspaceId) throws Exception;

    public TmNeuron loadNeuron(Long neuronId) throws Exception;

}
