package org.janelia.it.FlyWorkstation.model.domain;

import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.jacs.model.entity.Entity;

public class Annotation extends EntityWrapper {

    public Annotation(Entity entity) {
        super(new RootedEntity(entity));
    }
}
