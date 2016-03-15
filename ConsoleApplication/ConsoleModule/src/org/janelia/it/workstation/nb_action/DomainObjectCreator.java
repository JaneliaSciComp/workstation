package org.janelia.it.workstation.nb_action;

import org.janelia.it.jacs.model.domain.DomainObject;

/**
 * Implement this to make a means of creating an entity for viewing another
 * entity.  This generally involves wrapping the original entity.
 * @author fosterl
 */
public interface DomainObjectCreator extends Compatible<DomainObject> {
    public static final String LOOKUP_PATH = "DomainPerspective/DomainObjectCreator";
    
    void useDomainObject( DomainObject e );
    @Override
    boolean isCompatible( DomainObject e );
    String getActionLabel();
}
