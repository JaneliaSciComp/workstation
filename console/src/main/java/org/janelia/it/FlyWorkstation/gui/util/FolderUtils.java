package org.janelia.it.FlyWorkstation.gui.util;

import java.util.List;

import org.janelia.it.FlyWorkstation.api.entity_model.management.EntitySelectionModel;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Utilities for dealing with Folder entities. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FolderUtils {

	
	public static RootedEntity saveEntitiesToFolder(RootedEntity parentFolder, String folderName, List<Long> entityIds) throws Exception {
	
		if (parentFolder==null) {
			return saveEntitiesToCommonRoot(folderName, entityIds);
		}
		
		EntityData saveFolderEd = EntityUtils.findChildEntityDataWithName(parentFolder.getEntity(), folderName);
		
		Entity saveFolder = null;
		if (saveFolderEd == null) {
			// No existing folder, so create a new one
			saveFolder = ModelMgr.getModelMgr().createEntity(EntityConstants.TYPE_FOLDER, folderName);
			saveFolderEd = ModelMgr.getModelMgr().addEntityToParent(parentFolder.getEntity(), saveFolder, parentFolder.getEntity().getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
		}
		else {
			saveFolder = saveFolderEd.getChildEntity();	
		}
    	
        saveEntities(saveFolder, entityIds);
        return parentFolder.getChild(saveFolderEd);
	}
	
	public static RootedEntity saveEntitiesToCommonRoot(String commonRootName, List<Long> entityIds) throws Exception {

		Entity saveFolder = null;
		List<EntityData> rootEds = SessionMgr.getBrowser().getEntityOutline().getRootEntity().getOrderedEntityData();
		for(EntityData rootEd : rootEds) {
			final Entity commonRoot = rootEd.getChildEntity();
			if (!commonRoot.getUser().getUserLogin().equals(SessionMgr.getUsername())) continue;
			if (commonRoot.getName().equals(commonRootName)) {
				saveFolder = commonRoot;
			}
		}

		if (saveFolder == null) {
			// No existing folder, so create a new one
			saveFolder = ModelMgr.getModelMgr().createCommonRoot(commonRootName);
		}

		saveEntities(saveFolder, entityIds);
		return new RootedEntity(saveFolder);
	}
	
	public static void saveEntities(Entity saveFolder, List<Long> entityIds) throws Exception {
		ModelMgr.getModelMgr().addChildren(saveFolder.getId(), entityIds, EntityConstants.ATTRIBUTE_ENTITY);
	}	
	
	public static void selectCommonRoot(Entity commonRoot) {
		ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(EntitySelectionModel.CATEGORY_OUTLINE, "/e_"+commonRoot.getId(), true);
	}
}
