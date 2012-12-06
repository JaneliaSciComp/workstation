package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;


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
	
	public RootedEntity getChildByName(String childName) {
		return getChild(EntityUtils.findChildEntityDataWithName(getEntity(), childName));
	}
	
	public RootedEntity getLatestChildOfType(String entityTypeName) {
    	List<EntityData> eds = getEntity().getOrderedEntityData();
    	Collections.reverse(eds);
    	for(EntityData ed : eds) {
    		Entity child = ed.getChildEntity();
    		if (child!=null) {
	    		if (!child.getEntityType().getName().equals(entityTypeName)) continue;
	    		return getChild(ed);
    		}
    	}
    	return null;
	}
	
	public List<RootedEntity> getRootedChildren() {
		
		List<RootedEntity> children = new ArrayList<RootedEntity>();
		for(EntityData ed : getEntity().getEntityData()) {
			if (ed.getChildEntity()!=null) {
				children.add(getChild(ed));
			}
		}
		return children;
	}
}
