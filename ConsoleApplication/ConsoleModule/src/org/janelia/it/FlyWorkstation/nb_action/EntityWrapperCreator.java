package org.janelia.it.FlyWorkstation.nb_action;

import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;

/**
 * Implement this to make a means of creating an entity for viewing another
 * entity.  This generally involves wrapping the original entity.
 * @author fosterl
 */
public interface EntityWrapperCreator extends Compatible<RootedEntity> {
    public static final String LOOKUP_PATH = "EntityPerspective/EntityWrapperCreator";
    
    void wrapEntity( RootedEntity e );
    @Override
    boolean isCompatible( RootedEntity e );
    String getActionLabel();
}
