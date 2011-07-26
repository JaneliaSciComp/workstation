package org.janelia.it.FlyWorkstation.api.facade.abstract_facade.fundtype;

import org.janelia.it.FlyWorkstation.api.stub.data.NoData;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/25/11
 * Time: 4:06 PM
 */
public interface EntityLoader {
    EntityData[] getData
      (Long entityId);

    EntityData[] getProperties
     (Long entityId,
      EntityType dyanmicType,
      boolean deepLoad);

   EntityData[] expandProperty
     (Long entityId,
      String propertyName,
      EntityType dyanmicType,
      boolean deepLoad)
     throws NoData;

}
