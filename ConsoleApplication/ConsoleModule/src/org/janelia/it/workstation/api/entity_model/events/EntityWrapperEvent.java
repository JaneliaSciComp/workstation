package org.janelia.it.workstation.api.entity_model.events;

import org.janelia.it.workstation.model.domain.EntityWrapper;

/**
 * An event in the entity model affecting the given entity in some way.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityWrapperEvent {

	private EntityWrapper wrapper;
	
	public EntityWrapperEvent(EntityWrapper wrapper) {
		this.wrapper = wrapper;
	}

	public EntityWrapper getWrapper() {
		return wrapper;
	}
}
