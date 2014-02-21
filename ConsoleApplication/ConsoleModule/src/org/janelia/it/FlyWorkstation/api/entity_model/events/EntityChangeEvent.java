package org.janelia.it.FlyWorkstation.api.entity_model.events;

import org.janelia.it.jacs.model.entity.Entity;

/**
 * An entity has changed in some way. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityChangeEvent extends EntityEvent {
	public EntityChangeEvent(Entity entity) {
		super(entity);
	}
}
