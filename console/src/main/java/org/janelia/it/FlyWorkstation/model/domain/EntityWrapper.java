package org.janelia.it.FlyWorkstation.model.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.jacs.model.entity.Entity;

/**
 * A base class for domain-specific Entity wrappers, such as Folders and Samples. Encapsulates an entity context
 * for choosing what children or ancestors to load. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class EntityWrapper {
    
    private RootedEntity rootedEntity;
    private Entity entity;
    
    private List<EntityWrapper> children;
    private EntityWrapper parent;
    private List<Annotation> annotations;
    
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

    public List<EntityWrapper> getChildren() {
        return children;
    }
    
    public List<Annotation> retrieveAnnotations() throws Exception {
        if (annotations==null) {
            ModelMgr.getModelMgr().getAnnotationsForEntity(entity.getId());
            // TODO: populate them
        }
        return annotations;
    }
    
    protected void initChildren() {
        this.children = new ArrayList<EntityWrapper>();
    }
    
    /**
     * Override to load child in a given context. If you override this method, you MUST call initChildren, so that 
     * getChildren() starts returning a list instead of null.
     * @param alignmentContext
     * @throws Exception
     */
    public void loadContextualizedChildren(AlignmentContext alignmentContext) throws Exception {
        initChildren();
    }

    protected void addChild(EntityWrapper child) {
        child.setParent(this);
        if (children==null) {
            this.children = new ArrayList<EntityWrapper>();
        }
        children.add(child);
    }

    @Override
    public String toString() {
        return "EntityWrapper [id=" + getId() + ", name=" + getName() + ", type=" + getType()
                + ", role=" + getRole() + ", owner=" + getOwnerKey() + "]";
    }
    
    
}
