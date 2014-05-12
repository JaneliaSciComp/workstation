package org.janelia.it.FlyWorkstation.api.entity_model.events;

import java.util.Collection;

import org.janelia.it.jacs.model.entity.Entity;

/**
 * One or more cached entities have been invalidated.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityInvalidationEvent {
	
	private Collection<Entity> entities;

    public EntityInvalidationEvent() {
        this.entities = null;
    }
    
	public EntityInvalidationEvent(Collection<Entity> entities) {
		this.entities = entities;
	}

	public Collection<Entity> getInvalidatedEntities() {
		return entities;
	}
	
	public boolean isTotalInvalidation() {
	    return entities == null;
	}
}
