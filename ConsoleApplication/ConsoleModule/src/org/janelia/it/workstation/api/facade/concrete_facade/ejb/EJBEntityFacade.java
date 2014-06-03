package org.janelia.it.workstation.api.facade.concrete_facade.ejb;

import org.janelia.it.jacs.compute.api.support.MappedId;
import org.janelia.it.jacs.model.entity.*;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;
import org.janelia.it.workstation.api.facade.abstract_facade.EntityFacade;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
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
        return new ArrayList<Entity>(EJBFactory.getRemoteEntityBean().getEntitiesByName(SessionMgr.getSubjectKey(), entityName));
    }

    @Override
    public ArrayList<Entity> getOwnedEntitiesByName(String entityName) throws Exception {
        return new ArrayList<Entity>(EJBFactory.getRemoteEntityBean().getUserEntitiesByName(SessionMgr.getSubjectKey(), entityName));
    }

    @Override
    public List<Entity> getAlignmentSpaces() throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getAlignmentSpaces(SessionMgr.getSubjectKey());
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
    public List<EntityData> getAllParentEntityDatas(Long childEntityId) throws Exception {
        List<EntityData> list = new ArrayList<EntityData>();
        Set<EntityData> set = EJBFactory.getRemoteEntityBean().getParentEntityDatas(null, childEntityId);
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
        String user = SessionMgr.getSubjectKey();
        return EJBFactory.getRemoteEntityBean().getChildEntities(user, parentEntityId);
    }

    @Override
    public List<Entity> getEntitiesByTypeName(String entityTypeName) throws Exception {
        return EJBFactory.getRemoteEntityBean().getEntitiesByTypeName(SessionMgr.getSubjectKey(), entityTypeName);
    }
    
    @Override
    public List<Entity> getOwnedEntitiesByTypeName(String entityTypeName) throws Exception {
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
        EJBFactory.getRemoteEntityBean().deleteEntityTreeById(SessionMgr.getSubjectKey(), entityId);
    }
    
    @Override
    public void deleteEntityTree(Long entityId, boolean unlinkMultipleParents) throws Exception {
	    EJBFactory.getRemoteEntityBean().deleteEntityTreeById(SessionMgr.getSubjectKey(), entityId, unlinkMultipleParents);
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
    public EntityData updateChildIndex(EntityData entityData, Integer orderIndex) throws Exception {
        return EJBFactory.getRemoteEntityBean().updateChildIndex(SessionMgr.getSubjectKey(), entityData, orderIndex);
    }

    @Override
    public Entity updateChildIndexes(Entity entity) throws Exception {
        return EJBFactory.getRemoteEntityBean().saveOrUpdateEntityDatas(SessionMgr.getSubjectKey(), entity);
    }
    
    @Override
    public EntityData setOrUpdateValue(Long entityId, String attributeName, String value) throws Exception {
        return EJBFactory.getRemoteEntityBean().setOrUpdateValue(SessionMgr.getSubjectKey(), entityId, attributeName, value);
    }
    
    public Collection<EntityData> setOrUpdateValues(Collection<Long> entityIds, String attributeName, String value) throws Exception {
        return EJBFactory.getRemoteEntityBean().setOrUpdateValues(SessionMgr.getSubjectKey(), entityIds, attributeName, value);
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


    // Addition of the interface for the Tiled Microscope Data
    @Override
    public TmWorkspace createTiledMicroscopeWorkspace(Long parentId, Long brainSampleId, String name, String ownerKey) throws Exception {
        return EJBFactory.getRemoteTiledMicroscopeBean().createTiledMicroscopeWorkspace(parentId, brainSampleId, name, ownerKey);
    }

    @Override
    public TmNeuron createTiledMicroscopeNeuron(Long workspaceId, String name) throws Exception {
        return EJBFactory.getRemoteTiledMicroscopeBean().createTiledMicroscopeNeuron(workspaceId, name);
    }

    @Override
    public TmSample createTiledMicroscopeSample(String user, String sampleName, String pathToRenderFolder) throws Exception {
        return EJBFactory.getRemoteTiledMicroscopeBean().createTiledMicroscopeSample(user, sampleName, pathToRenderFolder);
    }

    @Override
    public TmGeoAnnotation addGeometricAnnotation(Long neuronId, Long parentAnnotationId, int index,
                                                  double x, double y, double z, String comment) throws Exception {
        return EJBFactory.getRemoteTiledMicroscopeBean().addGeometricAnnotation(neuronId, parentAnnotationId, index, x, y, z, comment);
    }

    @Override
    public void reparentGeometricAnnotation(TmGeoAnnotation annotation,
                                            Long newParentAnnotationID, TmNeuron neuron) throws Exception {
        EJBFactory.getRemoteTiledMicroscopeBean().reparentGeometricAnnotation(annotation, newParentAnnotationID, neuron);
    }

    @Override
    public void rerootNeurite(TmNeuron neuron, TmGeoAnnotation newRoot) throws Exception {
        EJBFactory.getRemoteTiledMicroscopeBean().rerootNeurite(neuron, newRoot);
    }

    @Override
    public void splitNeurite(TmNeuron neuron, TmGeoAnnotation newRoot) throws Exception {
        EJBFactory.getRemoteTiledMicroscopeBean().splitNeurite(neuron, newRoot);
    }

    @Override
    public void moveNeurite(TmGeoAnnotation annotation, TmNeuron newNeuron) throws Exception {
        EJBFactory.getRemoteTiledMicroscopeBean().moveNeurite(annotation, newNeuron);
    }

    @Override
    public void updateGeometricAnnotation(TmGeoAnnotation geoAnnotation,
                                          int index, double x, double y, double z, String comment) throws Exception {
        EJBFactory.getRemoteTiledMicroscopeBean().updateGeometricAnnotation(geoAnnotation, index, x, y, z, comment);
    }

    @Override
    public List<TmWorkspaceDescriptor> getWorkspacesForBrainSample(Long brainSampleId, String ownerKey) throws Exception {
        return EJBFactory.getRemoteTiledMicroscopeBean().getWorkspacesForBrainSample(brainSampleId, ownerKey);
    }

    @Override
    public List<TmNeuronDescriptor> getNeuronsForWorkspace(Long workspaceId, String ownerKey) throws Exception {
        return EJBFactory.getRemoteTiledMicroscopeBean().getNeuronsForWorkspace(workspaceId, ownerKey);
    }

    @Override
    public void removeWorkspacePreference(Long workspaceId, String key) throws Exception {
        EJBFactory.getRemoteTiledMicroscopeBean().removeWorkspacePreference(workspaceId, key);
    }

    @Override
    public void createOrUpdateWorkspacePreference(Long workspaceId, String key, String value) throws Exception {
        EJBFactory.getRemoteTiledMicroscopeBean().createOrUpdateWorkspacePreference(workspaceId, key, value);
    }

    @Override
    public void deleteNeuron(String ownerKey, Long neuronId) throws Exception {
        EJBFactory.getRemoteTiledMicroscopeBean().deleteNeuron(ownerKey, neuronId);
    }

    @Override
    public void deleteWorkspace(String ownerKey, Long workspaceId) throws Exception {
        EJBFactory.getRemoteTiledMicroscopeBean().deleteWorkspace(ownerKey, workspaceId);
    }

    @Override
    public void deleteGeometricAnnotation(Long geoId) throws Exception {
        EJBFactory.getRemoteTiledMicroscopeBean().deleteGeometricAnnotation(geoId);
    }

    @Override
    public TmWorkspace loadWorkspace(Long workspaceId) throws Exception {
        return EJBFactory.getRemoteTiledMicroscopeBean().loadWorkspace(workspaceId);
    }

    @Override
    public TmNeuron loadNeuron(Long neuronId) throws Exception {
        return EJBFactory.getRemoteTiledMicroscopeBean().loadNeuron(neuronId);
    }

    @Override
    public TmAnchoredPath addAnchoredPath(Long neuronID, Long annotationID1, Long annotationID2,
    List<List<Integer>> pointlist) throws Exception {
        return EJBFactory.getRemoteTiledMicroscopeBean().addAnchoredPath(neuronID, annotationID1,
                annotationID2, pointlist);
    }

    @Override
    public void updateAnchoredPath(TmAnchoredPath anchoredPath, Long annotationID1, Long annotationID2,
    List<List<Integer>> pointlist) throws Exception {
        EJBFactory.getRemoteTiledMicroscopeBean().updateAnchoredPath(anchoredPath, annotationID1,
                annotationID2, pointlist);
    }

    @Override
    public void deleteAnchoredPath(Long pathID) throws Exception {
        EJBFactory.getRemoteTiledMicroscopeBean().deleteAnchoredPath(pathID);
    }

}
