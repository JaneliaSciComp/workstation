package org.janelia.it.FlyWorkstation.model.viewer;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.FlyWorkstation.model.domain.EntityWrapper;
import org.janelia.it.FlyWorkstation.model.domain.EntityWrapperFactory;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

/**
 * An aligned item an alignment board context. Always has an entity, and may have other AlignedItems as children. 
 * Also provides contextual properties for displaying the alignment board, such as visibility and color. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AlignedItem extends EntityWrapper {
    
    private EntityWrapper cachedItem;
    
    public AlignedItem(RootedEntity rootedEntity) {
        super(rootedEntity);
    }

    @Override
    protected void loadContextualizedChildren() throws Exception {
        EntityData itemEd = rootedEntity.getEntity().getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_ITEM);
        if (itemEd!=null) {
            this.cachedItem = EntityWrapperFactory.wrap(rootedEntity.getChild(itemEd));
        }
        for(RootedEntity child : rootedEntity.getChildrenForAttribute(EntityConstants.ATTRIBUTE_ENTITY)) {
            children.add(new AlignedItem(child));
        }
    }
    
    public EntityWrapper getEntity() {
        return cachedItem;
    }
    
    public boolean isVisible() {
        return "true".equals(entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_VISIBILITY));
    }

    public void setIsVisible(boolean visible) throws Exception {
        entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_VISIBILITY, new Boolean(visible).toString());
        ModelMgr.getModelMgr().saveOrUpdateEntity(entity);
    }
    
    public String getColorHex() {
        return entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_COLOR);
    }

    public void setColorHex(String colorHex) throws Exception {
        entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_COLOR, colorHex);
        ModelMgr.getModelMgr().saveOrUpdateEntity(entity);
    }
}
