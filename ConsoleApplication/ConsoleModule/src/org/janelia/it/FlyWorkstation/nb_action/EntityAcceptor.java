package org.janelia.it.FlyWorkstation.nb_action;

import org.janelia.it.jacs.model.entity.Entity;

/**
 * Implement this to accept an entity for processing.
 * 
 * @author fosterl
 */
public interface EntityAcceptor {
    String getActionLabel();
    boolean isCompatible( Entity e );
    void acceptEntity( Entity e );
}
