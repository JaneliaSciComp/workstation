package org.janelia.it.FlyWorkstation.model.viewer;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.FlyWorkstation.model.domain.AlignmentContext;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlignmentBoardContext extends AlignedItem {

    private static final Logger log = LoggerFactory.getLogger(AlignmentBoardContext.class);
    
    private AlignmentContext context;
    private List<AlignedItem> alignedItems;
    
    public AlignmentBoardContext(RootedEntity rootedEntity) {
        super(rootedEntity);
        String as = getInternalEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_SPACE);
        String ores = getInternalEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION);
        String pres = getInternalEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION);
        this.context = new AlignmentContext(as, ores, pres);
    }

    @Override
    public void loadContextualizedChildren(AlignmentContext alignmentContext) throws Exception {

        log.trace("loading alignment board");
        initChildren();
        this.alignedItems = new ArrayList<AlignedItem>();
        
        ModelMgr.getModelMgr().loadLazyEntity(getInternalEntity(), false);
        
        RootedEntity rootedEntity = getInternalRootedEntity();
        
        for(RootedEntity child : rootedEntity.getChildrenForAttribute(EntityConstants.ATTRIBUTE_ITEM)) {
            log.trace("adding child: {}",child.getName());
            AlignedItem item = new AlignedItem(child);
            addChild(item);
            alignedItems.add(item);
        }
    }

    public AlignmentContext getAlignmentContext() {
        return context;
    }
    
    public List<AlignedItem> getAlignedItems() {
        return alignedItems;
    }
}
