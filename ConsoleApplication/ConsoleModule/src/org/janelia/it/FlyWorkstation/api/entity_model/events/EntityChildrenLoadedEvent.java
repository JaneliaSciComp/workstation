package org.janelia.it.FlyWorkstation.api.entity_model.events;

import org.janelia.it.jacs.model.entity.Entity;

/**
 * An entity's children have been loaded.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityChildrenLoadedEvent extends EntityEvent {
	public EntityChildrenLoadedEvent(Entity entity) {
		super(entity);
	}
}
