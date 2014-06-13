package org.janelia.it.workstation.api.entity_model.management;

import java.util.HashSet;
import java.util.Set;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.entity.AbstractEntityLoader;

/**
 * Client-side implementation of the entity loader interface, using the ModelMgr.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ModelMgrEntityLoader implements AbstractEntityLoader {

	public ModelMgrEntityLoader() {
	}
	
	@Override
	public Set<EntityData> getParents(Entity entity) throws Exception {
		return new HashSet<EntityData>(ModelMgr.getModelMgr().getParentEntityDatas(entity.getId()));
	}

	@Override
	public Entity populateChildren(Entity entity) throws Exception {
	    ModelMgr.getModelMgr().loadLazyEntity(entity, false);
		return entity;
	}	
}
