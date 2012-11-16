package org.janelia.it.FlyWorkstation.api.entity_model.events;

import org.janelia.it.jacs.model.entity.Entity;

/**
 * A new entity has been created.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityCreateEvent extends EntityEvent {
	public EntityCreateEvent(Entity entity) {
		super(entity);
	}
}
