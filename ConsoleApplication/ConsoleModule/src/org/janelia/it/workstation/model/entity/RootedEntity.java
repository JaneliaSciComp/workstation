package org.janelia.it.workstation.model.entity;

import java.util.*;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.workstation.api.entity_model.management.ModelMgrUtils;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
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
        this.uniqueId = entity == null ? "/" : "/e_" + entity.getId();
        this.entityData = new EntityData();
        entityData.setChildEntity(entity);
    }

    public String getId() {
        return getUniqueId() == null ? entityData.getChildEntity().getId() + "" : getUniqueId();
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public EntityData getEntityData() {
        return entityData;
    }

    public Entity getEntity() {
        if (entityData == null) {
            throw new IllegalStateException("RootedEntity has null EntityData");
        }
        return entityData.getChildEntity();
    }

    public Long getEntityId() {
        return entityData.getChildEntity() == null ? null : entityData.getChildEntity().getId();
    }

    public void setEntity(Entity entity) {
        entityData.setChildEntity(entity);
    }

    public String getName() {
        return getEntity().getName();
    }

    public String getType() {
        return getEntity().getEntityTypeName();
    }

    public String getRole() {
        if (getEntityData() == null) {
            return null;
        }
        return getEntityData().getEntityAttrName();
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
        if (childEd == null) {
            return null;
        }
        return new RootedEntity(getUniqueId() + "/ed_" + childEd.getId() + "/e_" + childEd.getChildEntity().getId(), childEd);
    }

    public RootedEntity getChildByName(String childName) {
        return getChild(EntityUtils.findChildEntityDataWithName(getEntity(), childName));
    }

    public RootedEntity getOwnedChildByName(String childName) {
        return getChild(EntityUtils.findChildEntityDataWithNameAndOwner(getEntity(), childName, SessionMgr.getSubjectKey()));
    }
    
    public RootedEntity getChildById(long childId) {
        return getChild(EntityUtils.findChildEntityDataWithChildId(getEntity(), childId));
    }

    public List<RootedEntity> getChildrenOfType(String typeName) {
        List<RootedEntity> items = new ArrayList<RootedEntity>();
        for (EntityData ed : ModelMgrUtils.getAccessibleEntityDatasWithChildren(getEntity())) {
            Entity child = ed.getChildEntity();
            if (typeName == null || typeName.equals(child.getEntityTypeName())) {
                items.add(getChild(ed));
            }
        }
        return items;
    }

    public RootedEntity getChildOfType(String typeName) {
        List<RootedEntity> children = getChildrenOfType(typeName);
        if (children.isEmpty()) {
            return null;
        }
        if (children.size() > 1) {
            log.warn("Expected single child of type {} for entity {}", typeName, getId());
        }
        return children.get(0);
    }

    public List<RootedEntity> getChildrenForAttribute(String attrName) {
        List<RootedEntity> items = new ArrayList<RootedEntity>();
        for (EntityData ed : ModelMgrUtils.getAccessibleEntityDatasWithChildren(getEntity())) {
            if (attrName == null || attrName.equals(ed.getEntityAttrName())) {
                items.add(getChild(ed));
            }
        }
        return items;
    }

    public RootedEntity getChildForAttribute(String attrName) {
        List<RootedEntity> children = getChildrenForAttribute(attrName);
        if (children.isEmpty()) {
            return null;
        }
        if (children.size() > 1) {
            log.warn("Expected single child of attr {} for entity {}", attrName, getId());
        }
        return children.get(0);
    }

    public RootedEntity getLatestChildOfType(String entityTypeName) {
        List<EntityData> eds = ModelMgrUtils.getAccessibleEntityDatasWithChildren(getEntity());
        Collections.reverse(eds);
        for (EntityData ed : eds) {
            Entity child = ed.getChildEntity();
            if (!child.getEntityTypeName().equals(entityTypeName)) {
                continue;
            }
            return getChild(ed);
        }
        return null;
    }

    public EntityData getEntityDataByAttributeName(String attributeName) {
        Set<EntityData> matchingData = new HashSet<EntityData>();
        for (EntityData ed : ModelMgrUtils.getAccessibleEntityDatas(getEntity())) {
            if (ed.getEntityAttrName().matches(attributeName)) {
                matchingData.add(ed);
            }
        }
        if (matchingData.isEmpty()) {
            return null;
        }
        if (matchingData.size() > 1) {
            log.warn("Expected single EntityData for attr {} for entity {}", attributeName, getId());
        }
        return matchingData.iterator().next();
    }

    public String getValueByAttributeName(String attributeName) {
        EntityData ed = getEntityDataByAttributeName(attributeName);
        if (ed == null) {
            return null;
        }
        return ed.getValue();
    }

    public List<RootedEntity> getRootedChildren() {

        List<RootedEntity> children = new ArrayList<RootedEntity>();
        for (EntityData ed : ModelMgrUtils.getAccessibleEntityDatasWithChildren(getEntity())) {
            children.add(getChild(ed));
        }
        return children;
    }
}
