package org.janelia.it.FlyWorkstation.shared.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.Hibernate;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

/**
 * Utilities for dealing with Entities.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityUtils {

	public static final void loadChild(Entity entity, String attrName) throws Exception {
    	EntityData ed = entity.getEntityDataByAttributeName(attrName);
    	if (ed != null) {
    		ed.setChildEntity(ModelMgr.getModelMgr().getEntityById(ed.getChildEntity().getId()+""));
    	}
	}

    public static boolean areLoaded(Collection<EntityData> eds) {
        for (EntityData entityData : eds) {
            if (!Hibernate.isInitialized(entityData.getChildEntity())) {
                return false;
            }
        }
        return true;
    }

    public static void loadLazyEntity(Entity entity, boolean recurse) {

        if (!EntityUtils.areLoaded(entity.getEntityData())) {
            Set<Entity> childEntitySet = ModelMgr.getModelMgr().getChildEntities(entity.getId());
            Map<Long, Entity> childEntityMap = new HashMap<Long, Entity>();
            for (Entity childEntity : childEntitySet) {
                childEntityMap.put(childEntity.getId(), childEntity);
            }

            // Replace the entity data with real objects
            for (EntityData ed : entity.getEntityData()) {
                if (ed.getChildEntity() != null) {
                    ed.setChildEntity(childEntityMap.get(ed.getChildEntity().getId()));
                }
            }
        }

        if (recurse) {
            for (EntityData ed : entity.getEntityData()) {
                if (ed.getChildEntity() != null) {
                    loadLazyEntity(ed.getChildEntity(), true);
                }
            }
        }
    }
    
    public static String getFilePath(Entity entity) {
    	if (entity == null) return null;
    	return entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
    }
    
    public static String getDefaultImageFilePath(Entity entity) {

    	String type = entity.getEntityType().getName();
    	String path = null;
    	
    	// If the entity is a 2D image, just return its path
		if (type.equals(EntityConstants.TYPE_IMAGE_2D)) {
			path = getFilePath(entity);
		}
    	
		if (path == null) {
	    	// If the entity has a default 2D image, just return that path
	    	path = getFilePath(entity.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE));
		}

		if (path == null) {
	    	// TODO: This is for backwards compatibility with old data. Remove this in the future.
	    	path = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH);
		}
    	
		return path;
    }

    public static String getAnyFilePath(Entity entity) {
    	String filePath = getFilePath(entity);
    	if (filePath != null) {
    		return filePath;
    	}
    	return getDefaultImageFilePath(entity);
    }
    
	
}
