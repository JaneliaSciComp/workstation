package org.janelia.it.workstation.api.entity_model.events;

import java.util.Set;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;

/**
 * An entity has been deleted, potentially from multiple parents.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityRemoveEvent extends EntityEvent {
    private Set<EntityData> parentEds;
    
	public EntityRemoveEvent(Entity entity, Set<EntityData> parentEds) {
		super(entity);
		this.parentEds = parentEds;
	}

    public Set<EntityData> getParentEds() {
        return parentEds;
    }
	
}
