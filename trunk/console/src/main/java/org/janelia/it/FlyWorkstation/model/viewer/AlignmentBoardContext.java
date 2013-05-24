package org.janelia.it.FlyWorkstation.model.viewer;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.model.domain.AlignmentContext;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlignmentBoardContext extends AlignedItem {

    private static final Logger log = LoggerFactory.getLogger(AlignmentBoardContext.class);

    private AlignmentContext context;
    
    public AlignmentBoardContext(RootedEntity rootedEntity) {
        super(rootedEntity);
        String as = getInternalEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_SPACE);
        String ores = getInternalEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION);
        String pres = getInternalEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION);
        this.context = new AlignmentContext(as, ores, pres);
    }

    @Override
    public void loadContextualizedChildren(AlignmentContext alignmentContext) throws Exception {

        log.debug("Loading contextualized children for alignment board '{}' (id={})",getName(),getId());
        initChildren();
        
        ModelMgr.getModelMgr().loadLazyEntity(getInternalEntity(), false);
        
        RootedEntity rootedEntity = getInternalRootedEntity();
        
        for(RootedEntity child : rootedEntity.getChildrenForAttribute(EntityConstants.ATTRIBUTE_ITEM)) {
            log.debug("Adding child item: {} (id={})",child.getName(),child.getId());
            AlignedItem item = new AlignedItem(child);
            addChild(item);
        }
    }

    public AlignmentContext getAlignmentContext() {
        return context;
    }
}
