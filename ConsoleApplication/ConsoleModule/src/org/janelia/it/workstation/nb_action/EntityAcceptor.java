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
    void acceptEntity( Entity e );
    /**
     * Space these apart by at least 100, to leave room for injected separators
     * and for later-stage additions of menu items after the fact.
     * 
     * @return expected ascending order key for this menu item.
     */
    Integer getOrder();
    boolean isPrecededBySeparator();
    boolean isSucceededBySeparator();
    @Override
    boolean isCompatible( Entity e );
}
