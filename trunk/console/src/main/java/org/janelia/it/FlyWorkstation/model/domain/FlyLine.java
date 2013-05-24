package org.janelia.it.FlyWorkstation.model.domain;

import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.jacs.model.entity.Entity;

public class FlyLine extends EntityWrapper {
    
    public FlyLine(Entity entity) {
        super(new RootedEntity(entity));
    }
}
