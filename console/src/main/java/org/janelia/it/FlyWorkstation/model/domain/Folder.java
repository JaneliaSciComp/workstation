package org.janelia.it.FlyWorkstation.model.domain;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Folder extends EntityWrapper {

    private static final Logger log = LoggerFactory.getLogger(Folder.class);
    
    public Folder(RootedEntity rootedEntity) {
        super(rootedEntity);
    }    

    @Override
    protected void loadContextualizedChildren() throws Exception {
        // TODO: in the future, this should only show samples which have results in this context, but that's currently
        // too compute-intensive
        
        children.clear();
        
        ModelMgr.getModelMgr().loadLazyEntity(entity, false);
        
        for(EntityData childEd : entity.getOrderedEntityData()) {
            if (childEd.getChildEntity()==null) continue;
            try {
                EntityWrapper child = EntityWrapperFactory.wrap(rootedEntity.getChild(childEd));
                child.setParent(this);
                children.add(child);
            }
            catch (IllegalArgumentException e) {
                log.warn("Can't add child: "+childEd.getChildEntity().getName()+", "+e);
            }
        }       
    }
    
    public boolean isCommonRoot() {
        return entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT)!=null;
    }
}
