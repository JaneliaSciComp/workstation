package org.janelia.it.FlyWorkstation.model.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.jacs.model.entity.Entity;

/**
 * A base class for domain-specific Entity wrappers, such as Folders and Samples. Encapsulates an entity context
 * for choosing what children or ancestors to load. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class EntityWrapper {
    
    protected RootedEntity rootedEntity;
    protected Entity entity;
    
    protected EntityContext context;
    protected EntityWrapper parent;
    protected List<EntityWrapper> children = new ArrayList<EntityWrapper>();
    protected List<Annotation> annotations;
    
    private boolean inited = false;
    
    public EntityWrapper(RootedEntity rootedEntity) {
        this.rootedEntity = rootedEntity;
        this.entity = rootedEntity.getEntity();
    }
    
    public Entity getInternalEntity() {
        return entity;
    }

    public RootedEntity getInternalRootedEntity() {
        return rootedEntity;
    }
    
    public Long getId() {
        return entity.getId();
    }
    
    public String getUniqueId() {
        return rootedEntity.getUniqueId();
    }
    
    public String getName() {
        return entity.getName();
    }

    public String getType() {
        if (entity.getEntityType()==null) return null;
        return entity.getEntityType().getName();
    }
    
    public String getRole() {
        if (rootedEntity==null) return null;
        if (rootedEntity.getEntityData()==null) return null;
        if (rootedEntity.getEntityData().getEntityAttribute()==null) return null;
        return rootedEntity.getEntityData().getEntityAttribute().getName();
    }
    
    public String getOwnerKey() {
        return entity.getOwnerKey();
    }

    public Date getCreationDate() {
        return entity.getCreationDate();
    }
    
    public Date getUpdatedDate() {
        return entity.getUpdatedDate();
    }

    public EntityWrapper getParent() {
        return parent;
    }

    public void setParent(EntityWrapper parent) {
        this.parent = parent;
    }

    public final void loadContextualizedChildren(EntityContext context) throws Exception {
        this.context = context;
        loadContextualizedChildren();
        this.inited = true;
    }
    
    public EntityContext getContext() {
        return context;
    }
    
    public boolean isInited() {
        return inited;
    }

    public List<EntityWrapper> getChildren() {
//        checkContext();
        return children;
    }
    
    public List<Annotation> retrieveAnnotations() throws Exception {
        if (annotations==null) {
            ModelMgr.getModelMgr().getAnnotationsForEntity(entity.getId());
            // TODO: populate them
        }
        return annotations;
    }
    
    protected void loadContextualizedChildren() throws Exception {
    }

    protected final void checkContext() {
        if (context==null) {
            throw new IllegalStateException("Context not initialized");
        }
    }
}
