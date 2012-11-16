package org.janelia.it.FlyWorkstation.api.entity_model.management;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

import org.janelia.it.FlyWorkstation.api.entity_model.events.EntityChangeEvent;
import org.janelia.it.FlyWorkstation.api.entity_model.events.EntityChildrenLoadedEvent;
import org.janelia.it.FlyWorkstation.api.entity_model.events.EntityCreateEvent;
import org.janelia.it.FlyWorkstation.api.entity_model.events.EntityRemoveEvent;
import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.EntityFacade;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Implements a unified Entity model for client-side operations. All changes to the model should go through
 * this class, as well as any entity accesses which need to maintain the entity in memory for more than one 
 * user interaction. 
 * 
 * A few guiding principles:
 * 
 * 1) The methods in this class can potentially access the data source, so they should ALWAYS be called from a worker 
 *    thread, never from the EDT. By contrast, all resulting events are queued on the EDT, so that GUI widgets can 
 *    immediately make use of the results.
 * 
 * 2) Only one instance of any given entity is ever maintained by this model. If the entity has changed, the existing
 *    instance is updated with new information. Duplicate instances are always discarded. The canonical instance (cached
 *    instance) is returned by most methods, and clients can synchronize with the cache by using those instances.
 * 
 * 3) Invalidating a given entity ensures that the next time that entity is requested it will be loaded anew from the
 *    data source. Invalidating does not remove an entity from the cache, because other clients may still be using it.
 *    
 * 4) Updates to the cache are atomic (synchronized to the EntityModel instance).
 * 
 * 5) Cached entities may expire at any time, if they are not referenced outside of the cache. Therefore, you should 
 *    never write any code that relies on entities being cached or uncached.
 *    
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityModel {

	private static final Logger log = LoggerFactory.getLogger(EntityModel.class);
	
	private final EntityFacade entityFacade;
	private final Cache<Long,Entity> entityCache;
	private final ConcurrentSkipListSet<Long> invalidated;
	
	public EntityModel() {
		this.entityFacade = FacadeManager.getFacadeManager().getEntityFacade();
		this.entityCache = CacheBuilder.newBuilder().softValues().build();
		this.invalidated = new ConcurrentSkipListSet<Long>();
	}

	/**
	 * Ensure the given entity is in the cache, or generate appropriate warnings/exceptions. 
	 * 
	 * @param entity
	 */
    private void checkIfCanonicalEntity(Entity entity) {
    	try {
	    	Entity presentEntity = entityCache.getIfPresent(entity.getId());
	    	if (presentEntity==null) {
	    		throw new IllegalStateException("Not in entity model: "+entity.getName()+" (id="+entity.getId()+")");
	    	}
	    	else if (presentEntity!=entity) {
	    		throw new IllegalStateException("EntityModel: Instance mismatch: "+entity.getName()+
	    				" (cached="+System.identityHashCode(presentEntity)+
	    				") vs (this="+System.identityHashCode(entity)+")");
	    	}
    	}
    	catch (Exception e) {
    		log.warn("Problem verifying entity model",e);
    	}
    }
	
	/**
	 * Put the entity in the cache, or update the cached entity if there is already a version in the cache. In the
	 * latter case, the updated cached instance is returned, and the argument instance can be discarded.
	 * 
	 * If the entity has children which are already cached, those references will be replaced with references to
	 * the cached instances.
	 * 
	 * This method may generate an EntityUpdated event. 
	 * 
	 * @param entity 
	 * @return canonical entity instance
	 */
	private Entity putOrUpdate(Entity entity) {
		synchronized (this) {
			Entity canonicalEntity = entityCache.getIfPresent(entity.getId());
			if (canonicalEntity!=null) {
				if (!EntityUtils.areEqual(canonicalEntity, entity)) {
					log.debug("Updating {} (@{})",canonicalEntity.getName(),System.identityHashCode(canonicalEntity));
					EntityUtils.updateEntity(canonicalEntity, entity);	
					notifyEntityChanged(canonicalEntity);
				}
			}
			else {
				canonicalEntity = entity;
				log.debug("Caching {} (@{})",entity.getName(),System.identityHashCode(entity));
				entityCache.put(entity.getId(), entity);
			}
			
			// Replace the child entities with ones that we already know about
			for(EntityData ed : canonicalEntity.getEntityData()) {
				if (ed.getChildEntity()!=null) {
					Entity child = entityCache.getIfPresent(ed.getChildEntity().getId());
					if (child!=null) {
						ed.setChildEntity(child);
					}
				}
			}

			return canonicalEntity;
		}
	}

	/**
	 * Call putOrUpdate recursively on an entity tree. 
	 * 
	 * @param entity
	 * @param recurse
	 * @return canonical entity instance
	 */
	private Entity putOrUpdate(Entity entity, boolean recurse) {
		Entity putEntity = putOrUpdate(entity);
		if (recurse) {
			for(EntityData ed : entity.getEntityData()) {
				if (ed.getChildEntity()!=null && EntityUtils.isInitialized(ed.getChildEntity())) {
					Entity child = putOrUpdate(ed.getChildEntity(), true);
					ed.setChildEntity(child);
				}
			}
		}
		return putEntity;
	}
	
	/**
	 * Call putOrUpdate on all the entities in a given collection. 
	 * 
	 * @param entities
	 * @return canonical entity instances
	 */
	private List<Entity> putOrUpdateAll(Collection<Entity> entities) {
		List<Entity> putEntities = new ArrayList<Entity>();
		for(Entity entity : entities) {
			putEntities.add(putOrUpdate(entity));
		}
		return putEntities;
	}
	
    /**
     * Invalidate the entity in the cache, so that it will be reloaded on the next request. 
     * 
     * @param entity
     * @param recurse
     */
	public void invalidate(Entity entity, boolean recurse) {
		if (recurse) {
			invalidate(entity, new HashSet<Long>());
		}
		else {
			if (!EntityUtils.isInitialized(entity)) return;
			invalidated.add(entity.getId());
		}
	}
	
	private void invalidate(Entity entity, Set<Long> visited) {
		if (visited.contains(entity.getId())) return;
		if (!EntityUtils.isInitialized(entity)) return;
		visited.add(entity.getId());
		invalidated.add(entity.getId());
		for(Entity child : entity.getChildren()) {
			invalidate(child, visited);
		}
	}
	
	/**
	 * Reload the given entity from the database, and update the cache. 
	 * 
	 * @param entity
	 * @return canonical entity instance
	 * @throws Exception
	 */
	public Entity reload(Entity entity) throws Exception {
		return reloadById(entity.getId());
	}

	/**
	 * Reload all the entities in the given collection, and update the cache.
	 * 
	 * @param entities
	 * @return canonical entity instances
	 * @throws Exception
	 */
	public List<Entity> reload(Collection<Entity> entities) throws Exception {
		return reloadById(EntityUtils.getEntityIdList(entities));
	}
	
	/**
	 * Reload the given entity from the database, and update the cache. 
	 * 
	 * @param entityId
	 * @return canonical entity instance
	 * @throws Exception
	 */
	public Entity reloadById(Long entityId) throws Exception {
		synchronized (this) {
			Entity entity = entityFacade.getEntityById(entityId+"");
			invalidated.remove(entity.getId());
			return putOrUpdate(entity);
		}
	}

	/**
	 * Reload all the entities in the given collection, and update the cache.
	 * 
	 * @param entityIds
	 * @return canonical entity instances
	 * @throws Exception
	 */
	public List<Entity> reloadById(List<Long> entityIds) throws Exception {
		synchronized (this) {
	        List<Entity> entities = entityFacade.getEntitiesById(entityIds);
	        invalidated.removeAll(entityIds);
	        return putOrUpdateAll(entities);
		}
	}

	/**
	 * Retrieve the given entity, checking the cache first, and then falling back to the database.
	 * 
	 * @param entityId
	 * @return canonical entity instance
	 * @throws Exception
	 */
	public Entity getEntityById(final long entityId) throws Exception {
		if (!invalidated.contains(entityId)) {
			Entity entity = entityCache.getIfPresent(entityId);
			if (entity!=null) {
				return entity;
			}
		}
		synchronized (this) {
			invalidated.remove(entityId);
			Entity entity = entityFacade.getEntityById(entityId+"");
			entity = putOrUpdate(entity);
			return entity;
		}
	}
	
	/**
	 * Retrieve the given entities, checking the cache first, and then falling back to the database.
	 * 
	 * @param entityIds
	 * @return canonical entity instances
	 * @throws Exception
	 */
    public List<Entity> getEntitiesById(Collection<Long> entityIds) throws Exception {
    	
    	Map<Long,Entity> entityMap = new HashMap<Long,Entity>();
    	List<Long> unfound = new ArrayList<Long>(entityIds);
    	
    	for(Iterator<Long> i = unfound.iterator(); i.hasNext(); ) {
    		Long entityId = i.next();
    		if (!invalidated.contains(entityId)) {
        		Entity e = entityCache.getIfPresent(entityId);
        		if (e!=null) {
            		entityMap.put(e.getId(),e);
            		i.remove();
        		}
    		}
    	}

    	if (!unfound.isEmpty()) {
			synchronized (this) {
		        List<Entity> remaining = entityFacade.getEntitiesById(unfound);
		        for(Entity entity : putOrUpdateAll(remaining)) {
			        entityMap.put(entity.getId(), entity);
		        }
		        invalidated.removeAll(entityIds);
			}
    	}
        
		// Order results in the way they were requested
		List<Entity> result = new ArrayList<Entity>();
		for(Long entityId : entityIds) {
			result.add(entityMap.get(entityId));
		}
		
        return result;
    }

    /**
     * Retrieve an entire entity tree from the database, and cache all the entities. 
     * 
     * @param entityId
     * @return canonical entity instances
     * @throws Exception
     */
	public Entity getEntityTree(long entityId) throws Exception {
		Entity entity = entityFacade.getEntityTree(entityId);
		return putOrUpdate(entity, true);
	}

	/**
	 * Retrieve entities with the given name from the database, and cache all the entities. 
	 * 
	 * @param entityName
	 * @return canonical entity instances
	 * @throws Exception
	 */
	public List<Entity> getEntitiesByName(String entityName) throws Exception {
		List<Entity> entities = FacadeManager.getFacadeManager().getEntityFacade().getEntitiesByName(entityName);	
		return putOrUpdateAll(entities);
	}
	
	/**
	 * Retrieve the children of the given entity from the database, and update the cache. 
	 * 
	 * @param entity
	 * @throws Exception
	 */
    public void refreshChildren(Entity entity) throws Exception {
    	checkIfCanonicalEntity(entity);
        synchronized (this) {
        	Set<Entity> childEntitySet = entityFacade.getChildEntities(entity.getId());
        	putOrUpdateAll(childEntitySet);
        	invalidated.removeAll(EntityUtils.getEntityIdList(childEntitySet));
        }
        putOrUpdate(entity);
        notifyEntityChildrenLoaded(entity);
    }
    
    /**
     * Retrieve the children (or ancestors) of the given entity from the database which are not loaded already, 
     * and update the cache. 
     * 
     * @param entity
     * @param recurse
     * @throws Exception
     */
    public void loadLazyEntity(Entity entity, boolean recurse) throws Exception {
        if (!EntityUtils.areLoaded(entity.getEntityData())) {
        	refreshChildren(entity);
        }
        if (recurse) {
            for (EntityData ed : entity.getEntityData()) {
                if (ed.getChildEntity() != null) {
                    loadLazyEntity(ed.getChildEntity(), true);
                }
            }
        }
    }
	
    /**
     * Create a new entity with the given type and name, and cache it.
     * 
     * @param entityTypeName
     * @param entityName
     * @return cache-normalized entity
     * @throws Exception
     */
    public Entity createEntity(String entityTypeName, String entityName) throws Exception {
    	Entity entity = null;
    	synchronized(this) {
	        entity = entityFacade.createEntity(entityTypeName, entityName);
	        entity = putOrUpdate(entity);
    	}
        notifyEntityCreated(entity);
        return entity;
    }

    /**
     * Rename the given entity, and update the cache.
     * 
     * @param entity
     * @param newName
     * @return cache-normalized entity
     * @throws Exception
     */
    public Entity renameEntity(Entity entity, String newName) throws Exception {
    	checkIfCanonicalEntity(entity);
    	synchronized(this) {
	    	Entity newEntity = entityFacade.getEntityById(entity.getId()+"");
	    	newEntity.setName(newName);
	    	entityFacade.saveEntity(newEntity);
	    	return putOrUpdate(newEntity);
    	}
    }
    
    /**
     * Set the given attribute as a tag (i.e. using the attribute name as the attribute value) on the specified 
     * entity, and update the cache.
     * 
     * @param entity
     * @param attributeName
     * @return cache-normalized entity
     * @throws Exception
     */
    public Entity setAttributeAsTag(Entity entity, String attributeName) throws Exception {
    	checkIfCanonicalEntity(entity);
    	synchronized(this) {
	    	entity.addAttributeAsTag(attributeName);
	        Entity newEntity = entityFacade.saveEntity(entity);
	    	return putOrUpdate(newEntity);
    	}
    }
    
    /**
     * Set the given attribute value on the specified entity, and update the cache.
     * 
     * @param entity
     * @param attributeName
     * @param attributeValue
     * @return cache-normalized entity
     * @throws Exception
     */
    public Entity setAttributeValue(Entity entity, String attributeName, String attributeValue) throws Exception {
    	checkIfCanonicalEntity(entity);
    	synchronized(this) {
	    	entity.setValueByAttributeName(attributeName, attributeValue);
	        Entity newEntity = entityFacade.saveEntity(entity);
	    	return putOrUpdate(newEntity);
    	}
    }
    
    /**
     * Delete the given EntityData object.
     * 
     * This method may generate an EntityChanged event.
     * 
     * @param entityData
     * @throws Exception
     */
    public void deleteEntityData(EntityData entityData) throws Exception {
    	Entity parent = null;
    	synchronized(this) {
	    	entityFacade.removeEntityData(entityData);
	    	if (entityData.getParentEntity()!=null) {
	    		parent = entityCache.getIfPresent(entityData.getParentEntity().getId());
	    		if (parent!=null) {
		    		parent.getEntityData().remove(entityData);
	    		}
	    	}
    	}
    	if (parent!=null) {
    		notifyEntityChanged(parent);
    	}
    }
    
    /**
     * Delete the given Entity object. 
     * 
     * This method may generate an EntityDeleted event. 
     * 
     * @param entity
     * @throws Exception
     */
    public void deleteEntity(Entity entity) throws Exception {
    	checkIfCanonicalEntity(entity);
    	synchronized(this) {
	    	entityFacade.deleteEntityById(entity.getId());
    		entityCache.invalidate(entity);
    	}
    	notifyEntityDeleted(entity);
    }

    /**
     * Delete the given Entity object tree. 
     * 
     * This method currently generates just one EntityDeleted event, for the root of the tree. 
     * 
     * @param entity
     * @throws Exception
     */
    public void deleteEntityTree(Entity entity) throws Exception {
    	checkIfCanonicalEntity(entity);
    	synchronized(this) {
	    	entityFacade.deleteEntityTree(entity.getId());
			entityCache.invalidate(entity);
    	}
    	notifyEntityDeleted(entity);
    }
    
    /**
     * Add the given Entity to the specified parent Entity, using the next available order index. 
     * 
     * This method generates a EntityChanged event on the parent.
     * 
     * @param parent
     * @param entity
     * @return
     * @throws Exception
     */
    public EntityData addEntityToParent(Entity parent, Entity entity) throws Exception {
    	return addEntityToParent(parent, entity, parent.getMaxOrderIndex()==null?0:parent.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
    }

    /**
     * Add the given Entity to the specified parent Entity, at the given order index.
     * 
     * This method generates a EntityChanged event on the parent.
     * 
     * @param parent
     * @param entity
     * @param index
     * @param attrName
     * @return
     * @throws Exception
     */
    public EntityData addEntityToParent(Entity parent, Entity entity, Integer index, String attrName) throws Exception {
    	checkIfCanonicalEntity(parent);
    	checkIfCanonicalEntity(entity);
    	EntityData ed = null;
    	synchronized(this) {
	    	ed = entityFacade.addEntityToParent(parent, entity, index, attrName);
	    	ed.setChildEntity(entity); // replace with a canonical entities
	    	ed.setParentEntity(parent);
	    	parent.getEntityData().add(ed);
    	}
    	notifyEntityChanged(parent);		
    	return ed;
    }

    /**
     * Add the given entities as children of the specified parent Entity. 
     * 
     * This method generates a EntityChanged event on the parent.
     * 
     * @param parentId
     * @param childrenIds
     * @param attributeName
     * @throws Exception
     */
	public void addChildren(Long parentId, List<Long> childrenIds, String attributeName) throws Exception {
    	synchronized(this) {
    		entityFacade.addChildren(parentId, childrenIds, attributeName);
    	}
    	notifyEntityChanged(reloadById(parentId));
	}
	
	/**
	 * Create a new common root folder and cache it. 
	 * 
	 * @param folderName
	 * @return cache-normalized entity
	 * @throws Exception
	 */
    public Entity createCommonRootFolder(String folderName) throws Exception {
    	Entity newFolder = null;
    	synchronized(this) {
	    	newFolder = entityFacade.createEntity(EntityConstants.TYPE_FOLDER, folderName);
			newFolder.addAttributeAsTag(EntityConstants.ATTRIBUTE_COMMON_ROOT);
			newFolder = entityFacade.saveEntity(newFolder);
    	}
		return putOrUpdate(newFolder);
    }
    
    /**
     * Demote a common root so that it becomes an ordinary folder.
     * 
     * @param commonRoot
     * @throws Exception
     */
    public void demoteCommonRootToFolder(Entity commonRoot) throws Exception {
    	checkIfCanonicalEntity(commonRoot);
    	synchronized(this) {
			EntityData rootTagEd = commonRoot.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT);
			checkArgument(rootTagEd!=null, "Entity is not a common root: "+commonRoot.getId());
			entityFacade.removeEntityData(rootTagEd);
    	}
    }
    
    /**
     * Returns all the common roots that the user has access to. 
     * 
     * @return cache-normalized entities
     * @throws Exception
     */
    public List<Entity> getCommonRoots() throws Exception {
		List<Entity> commonRoots = new ArrayList<Entity>();
    	synchronized (this) {
    		List<Entity> userRoots = entityFacade.getUserCommonRootEntitiesByTypeName(EntityConstants.TYPE_FOLDER);
    		commonRoots.addAll(putOrUpdateAll(userRoots));
    	}
		if (!"system".equals(SessionMgr.getUsername())) {
	    	synchronized (this) {
    			List<Entity> systemRoots = entityFacade.getSystemCommonRootEntitiesByTypeName(EntityConstants.TYPE_FOLDER);	
    			commonRoots.addAll(putOrUpdateAll(systemRoots));
	    	}
		
		}
		return commonRoots;
    }
    
	private void notifyEntityChildrenLoaded(Entity entity) {
		ModelMgr.getModelMgr().postOnEventBus(new EntityChildrenLoadedEvent(entity));
	}
	
	private void notifyEntityCreated(Entity entity) {
		ModelMgr.getModelMgr().postOnEventBus(new EntityCreateEvent(entity));
	}

	private void notifyEntityChanged(Entity entity) {
		ModelMgr.getModelMgr().postOnEventBus(new EntityChangeEvent(entity));
	}

	private void notifyEntityDeleted(Entity entity) {
		ModelMgr.getModelMgr().postOnEventBus(new EntityRemoveEvent(entity));
	}

}
