package org.janelia.it.FlyWorkstation.shared.util;

import java.util.*;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityActorPermission;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Utilities for dealing with Entities via the ModelMgr. In general, most of these methods access the database and 
 * should be called from a worker thread, not from the EDT. The exception are methods which describe the current user's
 * read/write access permissions to entities. Those used cached data and may be called from the EDT.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ModelMgrUtils {
	
	/**
	 * Returns the subject name part of a given subject key. For example, for "group:flylight", this will return "flylight".
	 * @param subjectKey
	 * @return
	 */
	public static String getNameFromSubjectKey(String subjectKey) {
		if (subjectKey==null) return null;
		return subjectKey.substring(subjectKey.indexOf(':')+1);
	}
    
	/**
	 * Returns true if the user owns the given entity. 
	 * @param entity
	 * @return
	 */
    public static boolean isOwner(Entity entity) {
    	if (entity==null) throw new IllegalArgumentException("Entity is null");
    	if (entity.getOwnerKey()==null) throw new IllegalArgumentException("Entity's owner is null");
    	return entity.getOwnerKey().equals(SessionMgr.getSubjectKey());
    }

    /**
     * Returns true if the user is authorized to read the given entity, either because they are the owner, or 
     * because they or one of their groups has read permission.
     * @param entity
     * @return
     */
	public static boolean hasReadAccess(Entity entity) {
		String ownerKey = entity.getOwnerKey();
		
		// Special case for fake entities which do not exist in the database
		if (ownerKey==null) return true;
		
		// User or any of their groups grant read access
		Set<String> subjectKeys = new HashSet<String>(SessionMgr.getSubjectKeys());
		if  (subjectKeys.contains(ownerKey)) return true;
		
		// Check explicit permission grants
		for(EntityActorPermission eap : entity.getEntityActorPermissions()) {
			if (subjectKeys.contains(eap.getSubjectKey())) {
				if (eap.getPermissions().contains("r")) {
					return true;
				}
			}
		}
		return false;
	}

    /**
     * Returns true if the user is authorized to write the given entity, either because they are the owner, or 
     * because they or one of their groups has write permission.
     * @param entity
     * @return
     */
	public static boolean hasWriteAccess(Entity entity) {
		String ownerKey = entity.getOwnerKey();
		
		// Special case for fake entities which do not exist in the database. 
		if (ownerKey==null) return false;
		
		// Only being the owner grants write access
		if (isOwner(entity)) return true;

		// Check explicit permission grants
		Set<String> subjectKeys = new HashSet<String>(SessionMgr.getSubjectKeys());
		for(EntityActorPermission eap : entity.getEntityActorPermissions()) {
			if (subjectKeys.contains(eap.getSubjectKey())) {
				if (eap.getPermissions().contains("w")) {
					return true;
				}
			}
		}
		return false;
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
			entity = ModelMgr.getModelMgr().loadLazyEntity(entity, false);
			parent.setEntity(entity);
		}
		EntityData repFolderEd = EntityUtils.findChildEntityDataWithName(entity, name);
		if (repFolderEd == null) {
			if (createIfMissing) {
				Entity repFolder = ModelMgr.getModelMgr().createEntity(EntityConstants.TYPE_FOLDER, name);
	            repFolderEd = ModelMgr.getModelMgr().addEntityToParent(entity, repFolder);
			}
			else {
				return null;
			}
		}
		
		RootedEntity child = parent.getChild(repFolderEd);
		if (!EntityUtils.areLoaded(child.getEntity().getEntityData())) {
			child.setEntity(ModelMgr.getModelMgr().loadLazyEntity(child.getEntity(), false));
		}
		return child;
	}

	/**
	 * Similar to Entity.getDescendantsOfType except it autoloads lazy children when necessary.
	 * @param entity
	 * @param typeName
	 * @param ignoreNested
	 * @return
	 */
    public static List<Entity> getDescendantsOfType(Entity entity, String typeName, boolean ignoreNested) throws Exception {
    	
    	boolean found = false;
        List<Entity> items = new ArrayList<Entity>();
        if (typeName==null || typeName.equals(entity.getEntityType().getName())) {
            items.add(entity);
            found = true;
        }
        
        if (!found || !ignoreNested) {
    		if (!EntityUtils.areLoaded(entity.getEntityData())) {
    			entity = ModelMgr.getModelMgr().loadLazyEntity(entity, !ignoreNested);
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

    public static void removeAllChildren(Entity entity) throws Exception {
    	List<EntityData> toDelete = new ArrayList<EntityData>();
        for (EntityData ed : new ArrayList<EntityData>(entity.getEntityData())) {
        	if (ed.getChildEntity()!=null) {
        		toDelete.add(ed);
        	}
        }
        ModelMgr.getModelMgr().deleteBulkEntityData(entity,toDelete);
    }
    
    /**
     * Update the ordering of the children of the given entity, to reflect the ordering provided by the comparator.
     * @param entity
     * @param comparator
     * @throws Exception
     */
    public static void fixOrderIndicies(Entity entity, Comparator<EntityData> comparator) throws Exception {
    	List<EntityData> orderedData = new ArrayList<EntityData>();
		for(EntityData ed : entity.getEntityData()) {
			if (ed.getChildEntity()!=null) {
				orderedData.add(ed);
			}
		}
    	Collections.sort(orderedData, comparator);
		
    	int orderIndex = 0;
		for(EntityData ed : orderedData) {
			if (ed.getOrderIndex()==null || orderIndex!=ed.getOrderIndex()) {
				ed.setOrderIndex(orderIndex);
			}
			orderIndex++;
		}
		ModelMgr.getModelMgr().saveOrUpdateEntity(entity);
    }
}
