package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;

/**
 * An entity with a context within an entity tree, rooted at a Common Root. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RootedEntity {
	
	private String uniqueId;
	private EntityData entityData;

	public RootedEntity(String uniqueId, EntityData entityData) {
		this.uniqueId = uniqueId;
		this.entityData = entityData;
	}
	
	public RootedEntity(Entity entity) {
		this.uniqueId = "/e_"+entity.getId();
		this.entityData = new EntityData();
		entityData.setChildEntity(entity);
	}

	public String getId() {
		return uniqueId==null ? entityData.getChildEntity().getId()+"" : uniqueId;
	}
	
	public String getUniqueId() {
		return uniqueId;
	}

	public EntityData getEntityData() {
		return entityData;
	}

	public Entity getEntity() {
		return entityData.getChildEntity();
	}
	
	public Long getEntityId() {
		return entityData.getChildEntity()==null?null:entityData.getChildEntity().getId();
	}
	
	public void setEntity(Entity entity) {
		entityData.setChildEntity(entity);
	}
	
	public RootedEntity getChild(EntityData childEd) {
		return new RootedEntity(getUniqueId()+"/ed_"+childEd.getId()+"/e_"+childEd.getChildEntity().getId(), childEd);
	}
}
