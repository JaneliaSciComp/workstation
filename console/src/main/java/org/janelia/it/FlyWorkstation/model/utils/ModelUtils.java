package org.janelia.it.FlyWorkstation.model.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.janelia.it.FlyWorkstation.model.domain.EntityWrapper;
import org.janelia.it.jacs.model.entity.Entity;

/**
 * Utilities for dealing with model objects.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ModelUtils {
	
    public static Collection<Entity> getInternalEntities(Collection<EntityWrapper> wrappers) {
        List<Entity> entities = new ArrayList<Entity>();
        for(EntityWrapper wrapper : wrappers) {
            entities.add(wrapper.getInternalEntity());
        }
        return entities;
    }
}
