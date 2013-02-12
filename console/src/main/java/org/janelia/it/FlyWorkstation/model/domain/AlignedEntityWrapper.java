package org.janelia.it.FlyWorkstation.model.domain;

import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.jacs.model.entity.Entity;

/**
 * Not sure this is needed at all. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class AlignedEntityWrapper extends EntityWrapper {
    
    public AlignedEntityWrapper(RootedEntity rootedEntity) {
        super(rootedEntity);
    }
}
