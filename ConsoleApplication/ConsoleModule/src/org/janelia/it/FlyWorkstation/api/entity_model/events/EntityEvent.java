package org.janelia.it.FlyWorkstation.api.entity_model.events;

import org.janelia.it.jacs.model.entity.Entity;

/**
 * An event in the entity model affecting the given entity in some way.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityEvent {

	private Entity entity;
	
	public EntityEvent(Entity entity) {
		this.entity = entity;
	}

	public Entity getEntity() {
		return entity;
	}
}
