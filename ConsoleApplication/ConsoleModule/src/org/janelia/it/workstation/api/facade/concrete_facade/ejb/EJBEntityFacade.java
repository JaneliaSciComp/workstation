package org.janelia.it.workstation.api.facade.concrete_facade.ejb;

import org.janelia.it.jacs.compute.api.support.MappedId;
import org.janelia.it.jacs.model.entity.*;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;
import org.janelia.it.workstation.api.facade.abstract_facade.EntityFacade;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;

import java.util.*;

import org.janelia.it.jacs.compute.api.TiledMicroscopeBeanRemote;
import org.janelia.it.jacs.model.user_data.UserToolEvent;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 8/5/11
 * Time: 10:49 AM
 */
public class EJBEntityFacade implements EntityFacade {
    public static final String COMM_FAILURE_MSG_TMB = "Communication failure.";
    private static final int RETRY_MAX_ATTEMPTS_RTMB = 5;
    private static final int RETRY_INTERIM_MULTIPLIER_RTMB = 500;

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
    public List<byte[]> getB64DecodedEntityDataValues(Long entityId, String entityDataType) throws Exception {
        return EJBFactory.getRemoteEntityBean().getB64DecodedEntityDataValues(entityId, entityDataType);
    }
    
    @Override
    public byte[] getB64DecodedEntityDataValue(Long entityId, Long entityDataId, String entityDataType) throws Exception {
        return EJBFactory.getRemoteEntityBean().getB64DecodedEntityDataValue(entityId, entityDataId, entityDataType);
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

    @Override
    public List<Entity> getWorkspaces() throws Exception {
    	return EJBFactory.getRemoteEntityBean().getWorkspaces(SessionMgr.getSubjectKey());
    }

    @Override
    public EntityData addRootToWorkspace(Long workspaceId, Long entityId) throws Exception {
    	return EJBFactory.getRemoteEntityBean().addRootToWorkspace(SessionMgr.getSubjectKey(), workspaceId, entityId);
    }
    
    @Override
    public EntityData createFolderInWorkspace(Long workspaceId, String entityName) throws Exception {
    	return EJBFactory.getRemoteEntityBean().createFolderInWorkspace(SessionMgr.getSubjectKey(), workspaceId, entityName);
    }

    // Addition of the interface for the Tiled Microscope Data
    @Override
    public TmWorkspace createTiledMicroscopeWorkspace(Long parentId, Long brainSampleId, String name, String ownerKey) throws Exception {
        TiledMicroscopeBeanRemote tmBean = getRemoteTMBWithRetries();
        if (tmBean == null) {
            throw new EJBLookupException("Communication failure.");
        }
        return tmBean.createTiledMicroscopeWorkspace(parentId, brainSampleId, name, ownerKey);
    }

    @Override
    public TmSample createTiledMicroscopeSample(String user, String sampleName, String pathToRenderFolder) throws Exception {
        TiledMicroscopeBeanRemote tmBean = getRemoteTMBWithRetries();
        if (tmBean == null) {
            throw new EJBLookupException("Communication failure.");
        }
        return tmBean.createTiledMicroscopeSample(user, sampleName, pathToRenderFolder);
    }

    @Override
    public void removeWorkspacePreference(Long workspaceId, String key) throws Exception {
        TiledMicroscopeBeanRemote tmBean = getRemoteTMBWithRetries();
        if (tmBean == null) {
            throw new EJBLookupException("Communication failure.");
        }
        tmBean.removeWorkspacePreference(workspaceId, key);
    }

    @Override
    public void createOrUpdateWorkspacePreference(Long workspaceId, String key, String value) throws Exception {
        TiledMicroscopeBeanRemote tmBean = getRemoteTMBWithRetries();
        if (tmBean == null) {
            throw new EJBLookupException(COMM_FAILURE_MSG_TMB);
        }
        tmBean.createOrUpdateWorkspacePreference(workspaceId, key, value);
    }

    @Override
    public TmWorkspace loadWorkspace(Long workspaceId) throws Exception {
        TiledMicroscopeBeanRemote tmBean = getRemoteTMBWithRetries();
        if (tmBean == null) {
            throw new EJBLookupException("Communication failure.");
        }
        return tmBean.loadWorkspace(workspaceId);
    }

    @Override
    public Map<Integer,byte[]> getTextureBytes( String basePath, int[] viewerCoord, int[] dimensions ) throws Exception {
        Map<Integer,byte[]> rtnVal = null;
        final TiledMicroscopeBeanRemote remoteTiledMicroscopeBean = getRemoteTMBWithRetries();
        if ( remoteTiledMicroscopeBean != null ) {
            rtnVal = remoteTiledMicroscopeBean.getTextureBytes( basePath, viewerCoord, dimensions );
        }
        return rtnVal;
    }
    
    @Override
    public RawFileInfo getNearestChannelFiles( String basePath, int[] viewerCoord ) throws Exception {
        RawFileInfo rtnVal = null;
        final TiledMicroscopeBeanRemote remoteTiledMicroscopeBean = getRemoteTMBWithRetries();
        if ( remoteTiledMicroscopeBean != null ) {
            rtnVal = remoteTiledMicroscopeBean.getNearestChannelFiles( basePath, viewerCoord );
        }
        return rtnVal;
    }

    @Override
    public CoordinateToRawTransform getLvvCoordToRawTransform( String basePath ) throws Exception {
        CoordinateToRawTransform rtnVal = null;
        final TiledMicroscopeBeanRemote remoteTiledMicroscopeBean = getRemoteTMBWithRetries();
        if ( remoteTiledMicroscopeBean != null ) {
            rtnVal = remoteTiledMicroscopeBean.getTransform(basePath);
        }
        return rtnVal;
    }
    
    public static TiledMicroscopeBeanRemote getRemoteTMBWithRetries() {
        TiledMicroscopeBeanRemote bean = null;
        for (int i = 0; i < RETRY_MAX_ATTEMPTS_RTMB; i++) {
            bean = EJBFactory.getRemoteTiledMicroscopeBean();
            if (bean != null) {
                if (i > 0) {
                    // At least one retry failed.
                    try {
                        UserToolEvent ute = new UserToolEvent();
                        ute.setAction(COMM_FAILURE_MSG_TMB);
                        ute.setCategory(TiledMicroscopeBeanRemote.class.getSimpleName());
                        ute.setToolName("EJB");
                        ute.setSessionId(0L);
                        ute.setTimestamp(new java.util.Date());
                        ute.setUserLogin("Unknown");
                        EJBFactory.getRemoteComputeBean().addEventToSessionAsync(null);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                break;
            }
            else {
                try {
                    Thread.sleep(RETRY_INTERIM_MULTIPLIER_RTMB * (i + 1));
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        };
        return bean;
    }
    
    public static class EJBLookupException extends Exception {
        public EJBLookupException(String message) {
            super(message);
        }
    }
}
