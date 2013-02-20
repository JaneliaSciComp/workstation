package org.janelia.it.FlyWorkstation.model.domain;

import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * An alignment space is a defined 3d volume for alignments. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AlignmentSpace extends EntityWrapper {

    public AlignmentSpace(Entity entity) {
        super(new RootedEntity(entity));
    }

    public String getAlignmentTargetFilepath() {
        return getInternalEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
    }

    @Override
    public String toString() {
        return "AlignmentSpace [id=" + getId() + ", name=" + getName() + "]";
    }   
}
