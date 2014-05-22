package org.janelia.it.workstation.api.entity_model.management;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

import java.util.Date;
import java.util.HashSet;

/**
 * Factory interface for creating specific Entity subtypes using a parameterized factory method (parameterized on EntityType).
 * All products must share the Entity interface.
 */
public class EntityFactory {
    /**
     * Tries to ensure that the only class that creates an instance of a
     * EntityFactory is the ModelMgr.
     */
    public EntityFactory(Integer creationKey) {
        if (creationKey.intValue() != ModelMgr.getModelMgr().hashCode())
            throw new IllegalStateException(" You must get the EntityFactory from the ModelMgr!! ");
    }

    public Entity create(Long id, String name, String type, String ownerKey, Entity parent) {
        Date date = new Date();
        Entity newEntity = null;
        Entity entityParent = null;
        if (parent != null) {
            entityParent = parent;
        }
        try {
            // todo Switch to Java 7 and use switch-case on strings!
            if (EntityConstants.TYPE_IMAGE_3D.equals(type)) {
            }
            else {
                newEntity = new Entity(id, name, ownerKey, type, date, date, new HashSet<EntityData>());
            }
        }
        catch (Exception fcEx) {
            org.janelia.it.workstation.api.facade.facade_mgr.FacadeManager.handleException(fcEx);
            newEntity = null;
        }

        return newEntity;
    }
}
