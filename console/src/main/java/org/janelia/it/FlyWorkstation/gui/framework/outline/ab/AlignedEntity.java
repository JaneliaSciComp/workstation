package org.janelia.it.FlyWorkstation.gui.framework.outline.ab;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * An aligned item which is backed by an Entity. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AlignedEntity implements AlignedItem {
    
    private Entity entity;

    public AlignedEntity(Entity entity) {
        this.entity = entity;
    }
    
    public Entity getEntity() {
        return entity;
    }
    
    public String getName() {
        return getEntity().getName();
    }
    
    public List<? extends AlignedItem> getChildren() {
        
        Entity entity = getEntity();
        List<AlignedEntity> children = new ArrayList<AlignedEntity>();
        
        if (EntityConstants.TYPE_SAMPLE.equals(entity.getEntityType().getName())) {
            Entity pipelineRun = EntityUtils.getLatestChildOfType(entity, EntityConstants.TYPE_PIPELINE_RUN);
            if (pipelineRun!=null) {
                Entity alignment = EntityUtils.getLatestChildOfType(entity, EntityConstants.TYPE_ALIGNMENT_RESULT);
                if (alignment!=null) {
                    Entity separation = EntityUtils.getLatestChildOfType(entity, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
                    if (separation!=null) {
                        Entity fragmentCollection = EntityUtils.getLatestChildOfType(entity, EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION);
                        for(Entity fragment : fragmentCollection.getOrderedChildren()) {
                            children.add(new AlignedEntity(fragment));
                        }
                    }
                }
            }
        }
     
        return children;
    }
    
    public String toString() {
        return getName();
    }
}
