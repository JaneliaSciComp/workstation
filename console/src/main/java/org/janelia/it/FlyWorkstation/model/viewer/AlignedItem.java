package org.janelia.it.FlyWorkstation.model.viewer;

import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.FlyWorkstation.model.domain.AlignedEntityWrapper;
import org.janelia.it.FlyWorkstation.model.domain.AlignedEntityWrapperFactory;
import org.janelia.it.FlyWorkstation.model.domain.EntityWrapper;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

public class AlignedItem extends EntityWrapper {
    
    private AlignedEntityWrapper cachedItem;
    
    public AlignedItem(RootedEntity rootedEntity) {
        super(rootedEntity);
    }

    @Override
    protected void loadContextualizedChildren() throws Exception {
        EntityData childEd = rootedEntity.getEntity().getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_ITEM);
        this.cachedItem = AlignedEntityWrapperFactory.wrap(rootedEntity.getChild(childEd));
    }
    
    public boolean isVisible() {
        return "true".equals(entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_VISIBILITY));
    }
    
    public String getColorHex() {
        return entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_COLOR);
    }

    public AlignedEntityWrapper getEntity() {
        return cachedItem;
    }
    
}
