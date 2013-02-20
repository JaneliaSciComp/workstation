package org.janelia.it.FlyWorkstation.model.viewer;

import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.FlyWorkstation.model.domain.AlignmentContext;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlignmentBoardContext extends AlignedItem {

    private static final Logger log = LoggerFactory.getLogger(AlignmentBoardContext.class);
    
    protected AlignmentContext context;
    
    public AlignmentBoardContext(RootedEntity rootedEntity) {
        super(rootedEntity);
        String as = getInternalEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_SPACE);
        String ores = getInternalEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION);
        String pres = getInternalEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION);
        this.context = new AlignmentContext(as, ores, pres);
    }

    @Override
    public void loadContextualizedChildren(AlignmentContext alignmentContext) throws Exception {

        initChildren();
        
        RootedEntity rootedEntity = getInternalRootedEntity();
        for(RootedEntity child : rootedEntity.getChildrenForAttribute(EntityConstants.ATTRIBUTE_ITEM)) {
            addChild(new AlignedItem(child));
        }
    }

    public AlignmentContext getAlignmentContext() {
        return context;
    }
}
