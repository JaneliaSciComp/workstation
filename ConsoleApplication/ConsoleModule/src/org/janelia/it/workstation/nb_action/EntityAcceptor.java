package org.janelia.it.workstation.nb_action;

import org.janelia.it.jacs.model.entity.Entity;

/**
 * Implement this to accept an entity for processing.
 * 
 * @author fosterl
 */
public interface EntityAcceptor extends Compatible<Entity> {
    public static final String PERSPECTIVE_CHANGE_LOOKUP_PATH = "EntityPerspective/EntityAcceptor/Nodes";
    String getActionLabel();
    @Override
    boolean isCompatible( Entity e );
    void acceptEntity( Entity e );
}
