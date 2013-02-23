package org.janelia.it.FlyWorkstation.model.viewer;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.model.domain.AlignmentContext;
import org.janelia.it.FlyWorkstation.model.domain.EntityWrapper;
import org.janelia.it.FlyWorkstation.model.domain.EntityWrapperFactory;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

/**
 * An aligned item an alignment board context. Always has an entity, and may have other AlignedItems as children. 
 * Also provides contextual properties for displaying the alignment board, such as visibility and color. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AlignedItem extends EntityWrapper {
    
    private EntityWrapper itemWrapper;
    
    public AlignedItem(RootedEntity rootedEntity) {
        super(rootedEntity);
    }

    @Override
    public void loadContextualizedChildren(AlignmentContext alignmentContext) throws Exception {
        
        // TODO: sanity check everything against the alignment context
        
        initChildren();
        ModelMgr.getModelMgr().loadLazyEntity(getInternalEntity(), false);
        
        RootedEntity rootedEntity = getInternalRootedEntity();
        EntityData itemEd = rootedEntity.getEntity().getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_ENTITY);
        if (itemEd!=null) {
            this.itemWrapper = EntityWrapperFactory.wrap(rootedEntity.getChild(itemEd));
        }
        
        for(RootedEntity child : rootedEntity.getChildrenForAttribute(EntityConstants.ATTRIBUTE_ITEM)) {
            addChild(new AlignedItem(child));
        }
    }
    
    public EntityWrapper getItemWrapper() {
        return itemWrapper;
    }
    
    public boolean isVisible() {
        Entity entity = getInternalEntity();
        return "true".equals(entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_VISIBILITY));
    }

    public void setIsVisible(boolean visible) throws Exception {
        Entity entity = getInternalEntity();
        entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_VISIBILITY, new Boolean(visible).toString());
        ModelMgr.getModelMgr().saveOrUpdateEntity(entity);
    }
    
    public String getColorHex() {
        Entity entity = getInternalEntity();
        return entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_COLOR);
    }

    public void setColorHex(String colorHex) throws Exception {
        Entity entity = getInternalEntity();
        entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_COLOR, colorHex);
        ModelMgr.getModelMgr().saveOrUpdateEntity(entity);
    }
}
