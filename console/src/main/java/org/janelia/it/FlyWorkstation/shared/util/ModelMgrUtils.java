package org.janelia.it.FlyWorkstation.shared.util;

import java.util.*;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Utilities for dealing with Entities via the ModelMgr. In general, most of these methods access the database and 
 * should be called from a worker thread, not from the EDT.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ModelMgrUtils {

	public static final void loadChild(Entity entity, String attrName) throws Exception {
    	EntityData ed = entity.getEntityDataByAttributeName(attrName);
    	if (ed != null) {
    		ed.setChildEntity(ModelMgr.getModelMgr().getEntityById(ed.getChildEntity().getId()+""));
    	}
	}

    public static void refreshEntityAndChildren(Entity entity) throws Exception {
    	ModelMgrUtils.updateEntity(entity, ModelMgr.getModelMgr().getEntityById(entity.getId()+""));
        Set<Entity> childEntitySet = ModelMgr.getModelMgr().getChildEntities(entity.getId());
        EntityUtils.replaceChildNodes(entity, childEntitySet);
    }
    
    public static void loadLazyEntity(Entity entity, boolean recurse) {

        if (!EntityUtils.areLoaded(entity.getEntityData())) {
            Set<Entity> childEntitySet = ModelMgr.getModelMgr().getChildEntities(entity.getId());
            EntityUtils.replaceChildNodes(entity, childEntitySet);
        }

        if (recurse) {
            for (EntityData ed : entity.getEntityData()) {
                if (ed.getChildEntity() != null) {
                    loadLazyEntity(ed.getChildEntity(), true);
                }
            }
        }
    }
    
    public static boolean isOwner(Entity entity) {
    	if (entity==null) throw new IllegalArgumentException("Entity is null");
    	if (entity.getUser()==null) {
    		throw new IllegalArgumentException("Entity's user is null");
    	}
    	return entity.getUser().getUserLogin().equals(SessionMgr.getUsername());
    }
    
    public static void updateEntity(Entity entity, Entity newEntity) {
	
		// Map old children onto new EDs, since the old children are initialized and the ones may not be
		Map<Long,Entity> childMap = new HashMap<Long,Entity>();
		for(EntityData ed : entity.getEntityData()) {
			if (ed.getChildEntity()!=null) {
				childMap.put(ed.getChildEntity().getId(), ed.getChildEntity());
			}
		}
		entity.setEntityData(newEntity.getEntityData());
		for(EntityData ed : entity.getEntityData()) {
			if (ed.getChildEntity()!=null && !EntityUtils.isInitialized(ed.getChildEntity())) {
				Entity child = childMap.get(ed.getChildEntity().getId());
				if (child!=null) {
					ed.setChildEntity(child);
				}
			}
		}
		
		entity.setName(newEntity.getName());
    	entity.setUpdatedDate(newEntity.getUpdatedDate());
		entity.setCreationDate(newEntity.getCreationDate());
		entity.setEntityStatus(newEntity.getEntityStatus());
		entity.setEntityType(newEntity.getEntityType());
		entity.setUser(newEntity.getUser());
		entity.setEntityData(newEntity.getEntityData());
    }
    
    public static Entity createNewCommonRoot(String folderName) throws Exception {
    	Entity newFolder = ModelMgr.getModelMgr().createEntity(EntityConstants.TYPE_FOLDER, folderName);
		newFolder.addAttributeAsTag(EntityConstants.ATTRIBUTE_COMMON_ROOT);
		return ModelMgr.getModelMgr().saveOrUpdateEntity(newFolder);
    }

    public static EntityData addChild(Entity parent, Entity child) throws Exception {
		EntityData newEntityData = parent.addChildEntity(child);
		EntityData savedEntityData = ModelMgr.getModelMgr().saveOrUpdateEntityData(newEntityData);
		newEntityData.setId(savedEntityData.getId());
		return savedEntityData;
    }

	public static RootedEntity getChildFolder(RootedEntity parent, String name, boolean createIfMissing) throws Exception {
		Entity entity = parent.getEntity();
		if (!EntityUtils.areLoaded(entity.getEntityData())) {
			ModelMgrUtils.loadLazyEntity(entity, false);
		}
		EntityData repFolderEd = EntityUtils.findChildEntityDataWithName(entity, name);
		if (repFolderEd == null) {
			if (createIfMissing) {
				Entity repFolder = ModelMgr.getModelMgr().createEntity(EntityConstants.TYPE_FOLDER, name);
				repFolder = ModelMgr.getModelMgr().saveOrUpdateEntity(repFolder);
	            repFolderEd = ModelMgr.getModelMgr().addEntityToParent(entity, repFolder, entity.getMaxOrderIndex()==null?0:entity.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
			}
			else {
				return null;
			}
		}
		
		RootedEntity child = parent.getChild(repFolderEd);
		return child;
	}

	/**
	 * Similar to Entity.getDescendantsOfType except it autoloads lazy children when necessary.
	 * @param entity
	 * @param typeName
	 * @param ignoreNested
	 * @return
	 */
    public static List<Entity> getDescendantsOfType(Entity entity, String typeName, boolean ignoreNested) {
    	
    	boolean found = false;
        List<Entity> items = new ArrayList<Entity>();
        if (typeName==null || typeName.equals(entity.getEntityType().getName())) {
            items.add(entity);
            found = true;
        }
        
        if (!found || !ignoreNested) {
    		if (!EntityUtils.areLoaded(entity.getEntityData())) {
    			ModelMgrUtils.loadLazyEntity(entity, !ignoreNested);
    		}
            for (EntityData entityData : entity.getOrderedEntityData()) {
                Entity child = entityData.getChildEntity();
                if (child != null) {
                    items.addAll(ModelMgrUtils.getDescendantsOfType(child, typeName, ignoreNested));
                }
            }	
        }

        return items;
    }
}
