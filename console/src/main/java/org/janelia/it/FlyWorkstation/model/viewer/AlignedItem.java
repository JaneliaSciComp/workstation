package org.janelia.it.FlyWorkstation.model.viewer;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.RenderMappingI;
import org.janelia.it.FlyWorkstation.model.domain.AlignmentContext;
import org.janelia.it.FlyWorkstation.model.domain.EntityWrapper;
import org.janelia.it.FlyWorkstation.model.domain.EntityWrapperFactory;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An aligned item an alignment board context. Always has an entity, and may have other AlignedItems as children. 
 * Also provides contextual properties for displaying the alignment board, such as visibility and color. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AlignedItem extends EntityWrapper {

    private static final Logger log = LoggerFactory.getLogger(AlignedItem.class);

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
            try {
                this.itemWrapper = EntityWrapperFactory.wrap(rootedEntity.getChild(itemEd));
            }
            catch (IllegalArgumentException e) {
                log.warn("Can't add child: "+itemEd.getChildEntity().getName()+", "+e);
            }
        }
        
        for(RootedEntity child : rootedEntity.getChildrenForAttribute(EntityConstants.ATTRIBUTE_ITEM)) {
            addChild(new AlignedItem(child));
        }
    }
    
    /**
     * Returns the underlying domain object (e.g. Sample, Neuron, Mask, etc) that is contextualized by this AlignedItem.
     * @return
     */
    public EntityWrapper getItemWrapper() {
        return itemWrapper;
    }

    /**
     * Returns the children of this aligned item. This basically just takes everything from getChildren() and casts it
     * to AlignedItem. 
     * @return
     */
    public List<AlignedItem> getAlignedItems() {
        if (getChildren()==null) {
            throw new IllegalStateException("This entity wrapper (name="+getName()+
            		",type="+getType()+") has not been initialized with children. " +
                    "Calling getAlignedItems will return null.");
        }
        List<AlignedItem> alignedItems = new ArrayList<AlignedItem>();
        for(EntityWrapper wrapper : getChildren()) {
            if (wrapper instanceof AlignedItem) {
                alignedItems.add((AlignedItem)wrapper);
            }
            else {
                log.warn("AlignedItems can only have other AlignedItems as children. " +
                		"In this case, we have a {} as a child of AlignedItem with id={}.",wrapper.getType(),getId());
            }
        }
        return alignedItems;
    }

    /**
     * Returns the child aligned item with the given entity id. This is the entity id of the wrapped domain entity,
     * NOT the AlignedItem entity.
     * @param entityId
     * @return
     */
    public AlignedItem getAlignedItemWithEntityId(Long entityId) {
        if (getChildren()==null) {
            log.warn("This entity wrapper (name={},type={}) has not been initialized with children. " +
            		"Calling getAlignedItemWithEntityId will return null.",getName(),getType());
            return null;
        }
        for(AlignedItem item : getAlignedItems()) {
            if (item.getItemWrapper().getId().equals(entityId)) {
                return item;
            }
        }
        return null;
    }
    
    /**
     * Returns true if this item is visible in the alignment board. 
     * @return
     */
    public boolean isVisible() {
        Entity entity = getInternalEntity();
        return "true".equals(entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_VISIBILITY));
    }

    /**
     * Sets the visibility of this item and immediately writes the database (therefore, this method should not be 
     * called in the EDT). 
     * @param visible
     * @throws Exception
     */
    public void setIsVisible(boolean visible) throws Exception {
        ModelMgr.getModelMgr().setOrUpdateValue(getInternalEntity(), EntityConstants.ATTRIBUTE_VISIBILITY, new Boolean(visible).toString());
    }
    
    /**
     * Returns the hex color of this item (e.g. "FF0000"). Returns null if no color has been set (a.k.a. use the native color). 
     * @return
     */
    public String getColorHex() {
        Entity entity = getInternalEntity();
        return entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_COLOR);
    }
    
    /**
     * Sets the color of this item and immediately writes the database (therefore, this method should not be 
     * called in the EDT). 
     * @param colorHex
     * @throws Exception
     */
    public void setColorHex(String colorHex) throws Exception {
        ModelMgr.getModelMgr().setOrUpdateValue(getInternalEntity(), EntityConstants.ATTRIBUTE_COLOR, colorHex);
    }

    /**
     * Returns the color of this item. Returns null if no color has been set (a.k.a. use the native color). 
     * @return
     */
    public Color getColor() {
        String colorHex = getColorHex();
        Color color = colorHex==null?null:Color.decode("#"+colorHex);
        return color;
    }

    /**
     * Sets the color of this item and immediately writes the database (therefore, this method should not be 
     * called in the EDT). 
     * @param color
     * @throws Exception
     */
    public void setColor(Color color) throws Exception {
        if ( color == null ) {
            setColorHex( null );
        }
        else {
            final String rgbHex = Integer.toHexString(color.getRGB()).substring(2);
            setColorHex(rgbHex);
        }
    }

    public boolean isPassthroughRendering() {
        Entity entity = getInternalEntity();
        return RenderMappingI.PASSTHROUGH_RENDER_ATTRIBUTE.equals(
                entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_RENDER_METHOD));
    }

    /**
     * Note: at time of writing, there is only one override being used, even though there are several possible
     * rendering attribute types.  If this is not used, the value will be Neuron or Compartment.
     *
     * @param passthroughRendering T to override rendering as passthrough
     * @throws Exception for the setter.
     */
    public void setPassthroughRendering(boolean passthroughRendering) throws Exception {
        String value = passthroughRendering ? RenderMappingI.PASSTHROUGH_RENDER_ATTRIBUTE : null;
        ModelMgr.getModelMgr().setOrUpdateValue(getInternalEntity(), EntityConstants.ATTRIBUTE_RENDER_METHOD, value);
    }


    /**
     * Removes an aligned entity from the board. This method must be called from a worker thread.
     * 
     * @param alignedItem
     * @throws Exception
     */
    public void findAndRemoveAlignedEntity(final AlignedItem alignedItem) {

        if (getChildren().contains(alignedItem)) {
            removeChild(alignedItem);
        }
        else {
            for(EntityWrapper entityWrapper : getChildren()) {
                if (entityWrapper instanceof AlignedItem) {
                    AlignedItem childAlignedItem = (AlignedItem)entityWrapper;
                    childAlignedItem.findAndRemoveAlignedEntity(alignedItem);
                }
            }
        }
    }
}
