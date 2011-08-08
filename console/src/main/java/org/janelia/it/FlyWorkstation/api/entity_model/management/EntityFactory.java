package org.janelia.it.FlyWorkstation.api.entity_model.management;

import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;

import java.util.Date;
import java.util.HashSet;

/**
 * Factory interface for creating specific Genomic Entity subtypes using
 * a parameterized factory method (parameterized on EntityType).
 * All products must share the GenomicEntity interface.
 */
public class EntityFactory {
    /**
     * Tries to ensure that the only class that creates an instance of a
     * GenomicEntityFactory is the ModelMgr.
     */
    public EntityFactory(Integer creationKey) {
        if (creationKey.intValue() != ModelMgr.getModelMgr().hashCode())
            throw new IllegalStateException(" You must get the EntityInterval Factory from the ModelMgr!! ");
    }

    public Entity create(Long oid, String displayName, EntityType type, Entity parentEntity) {
        Entity newEntity = null;
        long entityTypeValue = type.getId();

        Entity parent = null;
        try {
            if (EntityConstants.TYPE_FOLDER_ID == entityTypeValue) {
                newEntity = new Entity(oid, displayName, null, null, type, new Date(), new Date(), new HashSet<EntityData>());
            }
        }
        catch (Exception fcEx) {
            FacadeManager.handleException(fcEx);
            newEntity = null;
        }

        return newEntity;
    }

}
