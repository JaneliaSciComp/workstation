package org.janelia.it.workstation.api.entity_model.events;

/**
 * An event in the entity model affecting the given entity in some way.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityWrapperEvent {

	private org.janelia.it.workstation.model.domain.EntityWrapper wrapper;
	
	public EntityWrapperEvent(org.janelia.it.workstation.model.domain.EntityWrapper wrapper) {
		this.wrapper = wrapper;
	}

	public org.janelia.it.workstation.model.domain.EntityWrapper getWrapper() {
		return wrapper;
	}
}
