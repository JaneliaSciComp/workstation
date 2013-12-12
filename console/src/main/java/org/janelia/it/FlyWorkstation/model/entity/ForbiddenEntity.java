package org.janelia.it.FlyWorkstation.model.entity;

import java.security.AccessControlException;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.janelia.it.jacs.model.entity.*;

/**
 * A place-holder for an Entity that the current user cannot access.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ForbiddenEntity extends Entity {

	private Entity entity;
	
	public ForbiddenEntity(Entity entity) {
		this.entity = entity;
	}
	
	public Entity getEntity() {
		return entity;
	}

	@Override
	public Long getId() {
		return entity.getId();
	}

	@Override
	public void setId(Long id) {
		entity.setId(id);
	}

	@Override
	public String getEntityTypeName() {
		throw new AccessControlException("Access denied to entity "+entity.getId());
	}

	@Override
	public void setEntityTypeName(String entityTypeName) {
		throw new AccessControlException("Access denied to entity "+entity.getId());
	}

	@Override
	public Date getCreationDate() {
		throw new AccessControlException("Access denied to entity "+entity.getId());
	}

	@Override
	public void setCreationDate(Date creationDate) {
		throw new AccessControlException("Access denied to entity "+entity.getId());
	}

	@Override
	public Date getUpdatedDate() {
		throw new AccessControlException("Access denied to entity "+entity.getId());
	}

	@Override
	public void setUpdatedDate(Date updatedDate) {
		throw new AccessControlException("Access denied to entity "+entity.getId());
	}

	@Override
	public Set<EntityData> getEntityData() {
		throw new AccessControlException("Access denied to entity "+entity.getId());
	}

	@Override
	public void setEntityData(Set<EntityData> entityData) {
		super.setEntityData(entityData);
	}

	@Override
	public String getName() {
		throw new AccessControlException("Access denied to entity "+entity.getId());
	}

	@Override
	public void setName(String name) {
        throw new AccessControlException("Access denied to entity "+entity.getId());
	}

	@Override
	public String getOwnerKey() {
		throw new AccessControlException("Access denied to entity "+entity.getId());
	}

	@Override
	public void setOwnerKey(String ownerKey) {
		throw new AccessControlException("Access denied to entity "+entity.getId());
	}

	@Override
	public Set<EntityActorPermission> getEntityActorPermissions() {
		throw new AccessControlException("Access denied to entity "+entity.getId());
	}

	@Override
	public void setEntityActorPermissions(Set<EntityActorPermission> entityActorPermissions) {
		throw new AccessControlException("Access denied to entity "+entity.getId());
	}

	@Override
	public EntityData getEntityDataByAttributeName(String attributeName) {
		throw new AccessControlException("Access denied to entity "+entity.getId());
	}

	@Override
	public String getValueByAttributeName(String attributeName) {
		throw new AccessControlException("Access denied to entity "+entity.getId());
	}

	@Override
	public Entity getChildByAttributeName(String attributeName) {
		throw new AccessControlException("Access denied to entity "+entity.getId());
	}

	@Override
	public boolean setValueByAttributeName(String attributeName, String value) {
		throw new AccessControlException("Access denied to entity "+entity.getId());
	}

	@Override
	public EntityData addChildEntity(Entity entity) {
		throw new AccessControlException("Access denied to entity "+entity.getId());
	}

	@Override
	public EntityData addChildEntity(Entity entity, String attributeName) {
		throw new AccessControlException("Access denied to entity "+entity.getId());
	}

	@Override
	public List<EntityData> getList(String attributeName) {
		throw new AccessControlException("Access denied to entity "+entity.getId());
	}

	@Override
	public Integer getMaxOrderIndex() {
		throw new AccessControlException("Access denied to entity "+entity.getId());
	}

	@Override
	public List<EntityData> getOrderedEntityData() {
		throw new AccessControlException("Access denied to entity "+entity.getId());
	}

	@Override
	public List<Entity> getOrderedChildren() {
		throw new AccessControlException("Access denied to entity "+entity.getId());
	}

	@Override
	public boolean hasChildren() {
		throw new AccessControlException("Access denied to entity "+entity.getId());
	}

	@Override
	public Set<Entity> getChildren() {
		throw new AccessControlException("Access denied to entity "+entity.getId());
	}
	
	@Override
	public String toString() {
		return "Access denied";
	}
}
