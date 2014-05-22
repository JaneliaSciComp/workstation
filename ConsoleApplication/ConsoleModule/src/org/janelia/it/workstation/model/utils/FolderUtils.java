package org.janelia.it.workstation.model.utils;

import java.util.List;

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
	
	public static org.janelia.it.workstation.model.entity.RootedEntity saveEntitiesToFolder(org.janelia.it.workstation.model.entity.RootedEntity parentFolder, String folderName, List<Long> entityIds) throws Exception {
	
		if (parentFolder==null) {
			return saveEntitiesToCommonRoot(folderName, entityIds);
		}
		
		EntityData saveFolderEd = EntityUtils.findChildEntityDataWithName(parentFolder.getEntity(), folderName);
		
		Entity saveFolder = null;
		if (saveFolderEd == null) {
			// No existing folder, so create a new one
			saveFolder = org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().createEntity(EntityConstants.TYPE_FOLDER, folderName);
			saveFolderEd = org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().addEntityToParent(parentFolder.getEntity(), saveFolder, parentFolder.getEntity().getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
		}
		else {
			saveFolder = saveFolderEd.getChildEntity();	
		}
    	
        saveEntities(saveFolder, entityIds);
        return parentFolder.getChild(saveFolderEd);
	}
	
	public static org.janelia.it.workstation.model.entity.RootedEntity saveEntitiesToCommonRoot(String commonRootName, List<Long> entityIds) throws Exception {

		Entity saveFolder = null;
		List<Entity> commonRoots = org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().getCommonRootEntities();
		for(Entity commonRoot : commonRoots) {
		    if (!org.janelia.it.workstation.api.entity_model.management.ModelMgrUtils.hasWriteAccess(commonRoot)) continue;
			if (commonRoot.getName().equals(commonRootName)) {
				saveFolder = commonRoot;
			}
		}

		if (saveFolder == null) {
			// No existing folder, so create a new one
			saveFolder = org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().createCommonRoot(commonRootName);
		}

		saveEntities(saveFolder, entityIds);
		return new org.janelia.it.workstation.model.entity.RootedEntity(saveFolder);
	}
	
	public static void saveEntities(Entity saveFolder, List<Long> entityIds) throws Exception {
		org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().addChildren(saveFolder.getId(), entityIds, EntityConstants.ATTRIBUTE_ENTITY);
	}	
	
	public static void selectCommonRoot(Entity commonRoot) {
		org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(org.janelia.it.workstation.api.entity_model.management.EntitySelectionModel.CATEGORY_OUTLINE, "/e_"+commonRoot.getId(), true);
	}
}
