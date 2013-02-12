package org.janelia.it.FlyWorkstation.model.viewer;

import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.FlyWorkstation.model.domain.AlignmentContext;
import org.janelia.it.FlyWorkstation.model.domain.AlignmentSpace;
import org.janelia.it.FlyWorkstation.model.domain.EntityWrapper;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

public class AlignmentBoardContext extends EntityWrapper {

    protected AlignmentContext context;
    
    public AlignmentBoardContext(RootedEntity rootedEntity) {
        super(rootedEntity);
    }

    @Override
    protected void loadContextualizedChildren() throws Exception {
        AlignmentSpace space = new AlignmentSpace(entity.getChildByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_SPACE));
        String ores = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION);
        String pres = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION);
        this.context = new AlignmentContext(space, ores, pres);
        
        for(EntityData childEd : entity.getOrderedEntityData()) {
            children.add(new AlignedItem(rootedEntity.getChild(childEd)));
        }
    }

    public AlignmentContext getAlignmentContext() {
        return context;
    }
}
