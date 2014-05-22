package org.janelia.it.workstation.model.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.janelia.it.jacs.model.entity.Entity;

/**
 * Utilities for dealing with model objects.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ModelUtils {
	
    public static Collection<Entity> getInternalEntities(Collection<org.janelia.it.workstation.model.domain.EntityWrapper> wrappers) {
        List<Entity> entities = new ArrayList<Entity>();
        for(org.janelia.it.workstation.model.domain.EntityWrapper wrapper : wrappers) {
            entities.add(wrapper.getInternalEntity());
        }
        return entities;
    }
}
