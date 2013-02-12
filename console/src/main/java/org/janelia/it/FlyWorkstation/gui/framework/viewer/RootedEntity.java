package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.util.*;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An entity with a context within an entity tree, rooted at a Common Root. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RootedEntity {

    private static final Logger log = LoggerFactory.getLogger(RootedEntity.class);
    
	private String uniqueId;
	private EntityData entityData;

	public RootedEntity(String uniqueId, EntityData entityData) {
		this.uniqueId = uniqueId;
		this.entityData = entityData;
	}
	
	public RootedEntity(Entity entity) {
		this.uniqueId = "/e_"+entity.getId();
		this.entityData = new EntityData();
		entityData.setChildEntity(entity);
	}

	public String getId() {
		return getUniqueId()==null ? entityData.getChildEntity().getId()+"" : getUniqueId();
	}
	
	public String getUniqueId() {
		return uniqueId;
	}

	public EntityData getEntityData() {
		return entityData;
	}

	public Entity getEntity() {
		return entityData.getChildEntity();
	}
	
	public Long getEntityId() {
		return entityData.getChildEntity()==null?null:entityData.getChildEntity().getId();
	}
	
	public void setEntity(Entity entity) {
		entityData.setChildEntity(entity);
	}

    public String getName() {
        return getEntity().getName();
    }

    public String getType() {
        if (getEntity().getEntityType()==null) return null;
        return getEntity().getEntityType().getName();
    }
    
    public String getRole() {
        if (getEntityData()==null) return null;
        if (getEntityData().getEntityAttribute()==null) return null;
        return getEntityData().getEntityAttribute().getName();
    }
    
    public String getOwnerKey() {
        return getEntity().getOwnerKey();
    }

    public Date getCreationDate() {
        return getEntity().getCreationDate();
    }
    
    public Date getUpdatedDate() {
        return getEntity().getUpdatedDate();
    }
    
	public RootedEntity getChild(EntityData childEd) {
		return new RootedEntity(getUniqueId()+"/ed_"+childEd.getId()+"/e_"+childEd.getChildEntity().getId(), childEd);
	}
	
	public RootedEntity getChildByName(String childName) {
		return getChild(EntityUtils.findChildEntityDataWithName(getEntity(), childName));
	}
	
    public List<RootedEntity> getChildrenOfType(String typeName) {
        List<RootedEntity> items = new ArrayList<RootedEntity>();
        for (EntityData entityData : getEntity().getOrderedEntityData()) {
            Entity child = entityData.getChildEntity();
            if (child != null) {
                if (typeName==null || typeName.equals(child.getEntityType().getName())) {
                    items.add(getChild(entityData));
                }
            }
        }
        return items;
    }

    public RootedEntity getChildOfType(String typeName) {
        List<RootedEntity> children = getChildrenOfType(typeName);
        if (children.isEmpty()) return null;
        if (children.size()>1) {
            log.warn("Expected single child of type {} for entity {}", typeName, getId());
        }
        return children.get(0);
    }
    
    public List<RootedEntity> getChildrenForAttribute(String attrName) {
        List<RootedEntity> items = new ArrayList<RootedEntity>();
        for (EntityData entityData : getEntity().getOrderedEntityData()) {
            if (attrName==null || attrName.equals(entityData.getEntityAttribute().getName())) {
                Entity child = entityData.getChildEntity();
                if (child != null) {
                    items.add(getChild(entityData));
                }
            }
        }
        return items;
    }
    
    public RootedEntity getChildForAttribute(String attrName) {
        List<RootedEntity> children = getChildrenForAttribute(attrName);
        if (children.isEmpty()) return null;
        if (children.size()>1) {
            log.warn("Expected single child of attr {} for entity {}", attrName, getId());
        }
        return children.get(0);
    }
    
	public RootedEntity getLatestChildOfType(String entityTypeName) {
    	List<EntityData> eds = getEntity().getOrderedEntityData();
    	Collections.reverse(eds);
    	for(EntityData ed : eds) {
    		Entity child = ed.getChildEntity();
    		if (child!=null) {
	    		if (!child.getEntityType().getName().equals(entityTypeName)) continue;
	    		return getChild(ed);
    		}
    	}
    	return null;
	}

    public EntityData getEntityDataByAttributeName(String attributeName) {
        Set<EntityData> matchingData = new HashSet<EntityData>();
        for (EntityData ed : getEntity().getEntityData()) {
            if (ed.getEntityAttribute().getName().matches(attributeName)) {
                matchingData.add(ed);
            }
        }
        if (matchingData.isEmpty()) return null;
        if (matchingData.size() > 1) {
            log.warn("Expected single EntityData for attr {} for entity {}",attributeName,getId());
        }
        return matchingData.iterator().next();
    }

    public String getValueByAttributeName(String attributeName) {
        EntityData ed = getEntityDataByAttributeName(attributeName);
        if (ed == null) return null;
        return ed.getValue();
    }
    
	public List<RootedEntity> getRootedChildren() {
		
		List<RootedEntity> children = new ArrayList<RootedEntity>();
		for(EntityData ed : getEntity().getEntityData()) {
			if (ed.getChildEntity()!=null) {
				children.add(getChild(ed));
			}
		}
		return children;
	}
}
