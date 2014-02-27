package org.janelia.it.FlyWorkstation.nb_action;

import org.janelia.it.jacs.model.entity.Entity;

/**
 * Implement this to provide context for context-aware action.
 * 
 * @author fosterl
 */
public interface ContextSuitable {
    Entity getEntity();
}
