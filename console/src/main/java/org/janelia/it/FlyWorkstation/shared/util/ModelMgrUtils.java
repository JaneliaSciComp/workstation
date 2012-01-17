package org.janelia.it.FlyWorkstation.shared.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Utilities for dealing with Entities via the ModelMgr.
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
    
    public static boolean isOwner(Entity entity) {
    	return entity.getUser().getUserLogin().equals(SessionMgr.getUsername());
    }

}
