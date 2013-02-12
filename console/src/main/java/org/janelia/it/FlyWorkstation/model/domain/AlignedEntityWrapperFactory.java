package org.janelia.it.FlyWorkstation.model.domain;

import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.jacs.model.entity.EntityConstants;

public class AlignedEntityWrapperFactory {

    public static AlignedEntityWrapper wrap(RootedEntity rootedEntity) {
        String type = rootedEntity.getEntity().getEntityType().getName();
        if (EntityConstants.TYPE_FOLDER.equals(type)) {
            return new Folder(rootedEntity);
        }
        else if (EntityConstants.TYPE_SAMPLE.equals(type)) {
            return new Sample(rootedEntity);
        }
        throw new IllegalArgumentException("Cannot wrap entity type: "+type);
    }
}
