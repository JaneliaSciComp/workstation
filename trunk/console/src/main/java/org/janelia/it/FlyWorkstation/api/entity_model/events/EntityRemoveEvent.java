package org.janelia.it.FlyWorkstation.api.entity_model.events;

import org.janelia.it.jacs.model.entity.Entity;

/**
 * An entity has been deleted.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityRemoveEvent extends EntityEvent {
	public EntityRemoveEvent(Entity entity) {
		super(entity);
	}
}
