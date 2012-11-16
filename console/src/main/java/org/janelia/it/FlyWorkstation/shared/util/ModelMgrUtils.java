package org.janelia.it.FlyWorkstation.shared.util;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.user_data.User;
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

	// TODO: find usages and use ModelMgr directly
    public static void refreshEntityAndChildren(Entity entity) throws Exception {
    	ModelMgr.getModelMgr().refreshEntityAndChildren(entity);
    }
	// TODO: find usages and use ModelMgr directly
    public static void loadLazyEntity(Entity entity, boolean recurse) throws Exception {
    	ModelMgr.getModelMgr().loadLazyEntity(entity, recurse);
    }
    
    public static boolean isOwner(Entity entity) {
    	if (entity==null) throw new IllegalArgumentException("Entity is null");
    	if (entity.getUser()==null) {
    		throw new IllegalArgumentException("Entity's user is null");
    	}
    	return entity.getUser().getUserLogin().equals(SessionMgr.getUsername());
    }

	public static boolean hasAccess(Entity entity) {
		String ul = entity.getUser().getUserLogin();
		return (User.SYSTEM_USER_LOGIN.equals(ul) || SessionMgr.getUsername().equals(ul));
	}
	
	public static boolean hasAccess(EntityData entityData) {
		String ul = entityData.getUser().getUserLogin();
		return (User.SYSTEM_USER_LOGIN.equals(ul) || SessionMgr.getUsername().equals(ul));
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
			ModelMgr.getModelMgr().loadLazyEntity(entity, false);
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
			ModelMgr.getModelMgr().loadLazyEntity(child.getEntity(), false);
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
    			ModelMgr.getModelMgr().loadLazyEntity(entity, !ignoreNested);
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
