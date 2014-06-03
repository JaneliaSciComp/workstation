package org.janelia.it.workstation.api.entity_model.management;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

import org.janelia.it.workstation.api.entity_model.events.EntityChangeEvent;
import org.janelia.it.workstation.api.entity_model.events.EntityChildrenLoadedEvent;
import org.janelia.it.workstation.api.entity_model.events.EntityCreateEvent;
import org.janelia.it.workstation.api.entity_model.events.EntityInvalidationEvent;
import org.janelia.it.workstation.api.entity_model.events.EntityRemoveEvent;
import org.janelia.it.workstation.api.facade.abstract_facade.AnnotationFacade;
import org.janelia.it.workstation.api.facade.abstract_facade.EntityFacade;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.ForbiddenEntity;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.workstation.api.facade.abstract_facade.OntologyFacade;
import org.janelia.it.workstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.model.entity.RootedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Implements a unified Entity model for client-side operations. All changes to the model should go through this class, as well as any entity accesses
 * which need to maintain the entity in memory for more than one user interaction.
 *
 * A few guiding principles:
 *
 * 1) The methods in this class can potentially access the data source, so they should ALWAYS be called from a worker thread, never from the EDT. By
 * contrast, all resulting events are queued on the EDT, so that GUI widgets can immediately make use of the results.
 *
 * 2) Only one instance of any given entity is ever maintained by this model. If the entity has changed, the existing instance is updated with new
 * information. Duplicate instances are always discarded. The canonical instance (cached instance) is returned by most methods, and clients can
 * synchronize with the cache by using those instances.
 *
 * 3) Invalidating a given entity removes it from the cache entirely. Clients are encouraged to listen for the corresponding EntityInvalidationEvent,
 * and discard their references to invalidated entities.
 *
 * 4) Updates to the cache are atomic (synchronized to the EntityModel instance).
 *
 * 5) Cached entities may expire at any time, if they are not referenced outside of the cache. Therefore, you should never write any code that relies
 * on entities being cached or uncached.
 *
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityModel {

    private static final Logger log = LoggerFactory.getLogger(EntityModel.class);

    private final AnnotationFacade annotationFacade;
    private final OntologyFacade ontologyFacade;
    private final EntityFacade entityFacade;
    private final Cache<Long, Entity> entityCache;
    private final Map<Long, Entity> workspaceCache;
    private final Map<Long, Entity> ontologyRootCache;
    private final Multimap<Long, Long> parentMap;

    public EntityModel() {
        this.annotationFacade = FacadeManager.getFacadeManager().getAnnotationFacade();
        this.ontologyFacade = FacadeManager.getFacadeManager().getOntologyFacade();
        this.entityFacade = FacadeManager.getFacadeManager().getEntityFacade();
        this.entityCache = CacheBuilder.newBuilder().softValues().removalListener(new RemovalListener<Long, Entity>() {
            @Override
            public void onRemoval(RemovalNotification<Long, Entity> notification) {
                synchronized (EntityModel.this) {
                    Long id = notification.getKey();
                    Entity entity = notification.getValue();
                    log.trace("Removing evicted entity from parentMap: {}", EntityUtils.identify(entity));
                    parentMap.removeAll(id);
                    if (workspaceCache.containsKey(id)) {
                        log.trace("Removed entity was a workspace, clearing the workspace cache...");
                        workspaceCache.clear();
                    }
                    if (ontologyRootCache.containsKey(id)) {
                        log.trace("Removed entity was a common root, clearing the ontology root cache...");
                        ontologyRootCache.clear();
                    }
                }
            }
        }).build();
        this.workspaceCache = new LinkedHashMap<Long, Entity>();
        this.ontologyRootCache = new LinkedHashMap<Long, Entity>();
        this.parentMap = HashMultimap.<Long, Long>create();
    }

    /**
     * Ensure the given entity is in the cache, or generate appropriate warnings/exceptions.
     *
     * @param entity
     */
    private void checkIfCanonicalEntity(Entity entity) {
        try {
            Entity presentEntity = entityCache.getIfPresent(entity.getId());
            if (presentEntity == null) {
                throw new IllegalStateException("Not in entity model: " + EntityUtils.identify(entity));
            }
            else if (presentEntity != entity) {
                throw new IllegalStateException("EntityModel: Instance mismatch: " + entity.getName()
                        + " (cached=" + System.identityHashCode(presentEntity)
                        + ") vs (this=" + System.identityHashCode(entity) + ")");
            }
        }
        catch (Exception e) {
            log.warn("Problem verifying entity model", e);
        }
    }

    /**
     * Put the entity in the cache, or update the cached entity if there is already a version in the cache. In the latter case, the updated cached
     * instance is returned, and the argument instance can be discarded.
     *
     * If the entity has children which are already cached, those references will be replaced with references to the cached instances.
     *
     * This method may generate an EntityUpdated event.
     *
     * @param entity
     * @return canonical entity instance
     */
    private Entity putOrUpdate(Entity entity) {
        if (entity == null || !EntityUtils.isInitialized(entity) || entity.getEntityTypeName() == null) {
            // This is an uninitialized entity, which cannot go into the cache
            log.trace("putOrUpdate: entity is null or uninitialized");
            return null;
        }
        if (!EntityUtils.isInitialized(entity.getEntityActorPermissions())) {
            log.warn("putOrUpdate: entity permissions are uninitialized");
            return null;
        }

        synchronized (this) {
            Entity canonicalEntity = entityCache.getIfPresent(entity.getId());
            if (canonicalEntity != null) {
                if (!EntityUtils.areEqual(canonicalEntity, entity)) {
                    log.trace("putOrUpdate: Updating cached instance: {}", EntityUtils.identify(canonicalEntity));
                    EntityUtils.updateEntity(canonicalEntity, entity);
                    notifyEntityChanged(canonicalEntity);
                }
                else {
                    log.trace("putOrUpdate: Returning cached instance: {}", EntityUtils.identify(canonicalEntity));
                }
            }
            else {
                canonicalEntity = entity;
                log.trace("putOrUpdate: Caching: {}", EntityUtils.identify(canonicalEntity));
                entityCache.put(entity.getId(), entity);
            }

            // Ensure all EntityDatas have their parent set correctly
            for (EntityData ed : canonicalEntity.getEntityData()) {
                ed.setParentEntity(canonicalEntity);
            }

            // Replace existing parents' children
            for (Long parentId : parentMap.get(canonicalEntity.getId())) {
                Entity parent = entityCache.getIfPresent(parentId);
                if (parent != null) {
                    for (EntityData ed : parent.getEntityData()) {
                        if (ed.getChildEntity() != null && ed.getChildEntity().getId().equals(canonicalEntity.getId())) {
                            //log.trace("putOrUpdate: Replacing child {} on existing parent {}",EntityUtils.identify(canonicalEntity), EntityUtils.identify(parent));
                            ed.setChildEntity(canonicalEntity);
                        }
                    }
                }
            }

            // Replace the child entities with ones that we already know about
            for (EntityData ed : canonicalEntity.getEntityData()) {
                if (ed.getChildEntity() != null) {
                    Entity child = entityCache.getIfPresent(ed.getChildEntity().getId());
                    if (child != null) {
                        ed.setChildEntity(child);
                        //log.trace("putOrUpdate: Storing parent {} for child {}",EntityUtils.identify(canonicalEntity), EntityUtils.identify(child));
                        parentMap.put(child.getId(), canonicalEntity.getId());
                    }
                }
            }

            // Sanity check
            if (EntityUtils.areLoaded(entity.getEntityData())) {
                int c = 0;
                for (EntityData ed : entity.getEntityData()) {
                    if (ed.getChildEntity() != null) {
                        c++;
                    }
                }
                if (entity.getNumChildren() != null && entity.getNumChildren() != c) {
                    log.warn("Denormalized numChildren (" + entity.getNumChildren()
                            + ") does not match actual number of children (" + c + ") for entity " + entity.getId());
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
        if (entity instanceof ForbiddenEntity) {
            log.debug("Forbidden entity in tree: " + entity.getId());
            return entity;
        }
        if (recurse) {
            try {
                for (EntityData ed : entity.getEntityData()) {
                    if (ed.getChildEntity() != null && EntityUtils.isInitialized(ed.getChildEntity())) {
                        Entity child = putOrUpdate(ed.getChildEntity(), true);
                        ed.setChildEntity(child);
                    }
                }
            }
            catch (Throwable e) {
                log.error("Error trying to walk entity " + entity.getId());
                SessionMgr.getSessionMgr().handleException(e);
            }
        }
        return putOrUpdate(entity);
    }

    /**
     * Call putOrUpdate on all the entities in a given collection.
     *
     * @param entities
     * @return canonical entity instances
     */
    private List<Entity> putOrUpdateAll(Collection<Entity> entities) {
        List<Entity> putEntities = new ArrayList<Entity>();
        for (Entity entity : entities) {
            putEntities.add(putOrUpdate(entity));
        }
        return putEntities;
    }

    /**
     * Clear the entire cache without raising any events. This is basically only useful for changing logins.
     */
    public void invalidateAll() {
        entityCache.invalidateAll();
        workspaceCache.clear();
        ontologyRootCache.clear();
        parentMap.clear();
        ModelMgr.getModelMgr().postOnEventBus(new EntityInvalidationEvent());
    }

    /**
     * Invalidate any number of entities in the cache, so that they will be reloaded on the next request.
     *
     * @param entityIds
     */
    public void invalidate(Collection<Long> entityIds) {
        ImmutableMap<Long, Entity> presentMap = entityCache.getAllPresent(entityIds);
        invalidate(presentMap.values(), false);
    }

    /**
     * Invalidate the entity in the cache, so that it will be reloaded on the next request.
     *
     * @param entity
     * @param recurse
     */
    public void invalidate(Entity entity, boolean recurse) {
        List<Entity> entities = new ArrayList<Entity>();
        entities.add(entity);
        invalidate(entities, recurse);
    }

    /**
     * Invalidate any number of entities in the cache, so that they will be reloaded on the next request.
     *
     * @param entities
     * @param recurse
     */
    public void invalidate(Collection<Entity> entities, boolean recurse) {
        Map<Long, Entity> visited = new HashMap<Long, Entity>();
        invalidate(entities, visited, recurse);
        notifyEntitiesInvalidated(visited.values());
    }

    /**
     * Invalidate any number of entities in the cache, so that they will be reloaded on the next request.
     *
     * @param entities
     * @param recurse
     */
    private void invalidate(Collection<Entity> entities, Map<Long, Entity> visited, boolean recurse) {
        log.debug("Invalidating {} entities (recurse={})", entities.size(), recurse);
        for (Entity entity : entities) {
            invalidate(entity, visited, recurse);
        }
    }

    private void invalidate(Entity entity, Map<Long, Entity> visited, boolean recurse) {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("EntityModel illegally called from EDT!");
        }
        if (visited.containsKey(entity.getId())) {
            return;
        }
        if (!EntityUtils.isInitialized(entity)) {
            return;
        }
        if (entity instanceof ForbiddenEntity) {
            return;
        }
        checkIfCanonicalEntity(entity);
        log.debug("Invalidating cached instance {}", EntityUtils.identify(entity));
        visited.put(entity.getId(), entity);

        // Make a copy of the parent set because invalidate() will clear it
        Collection<Long> parentIds = new HashSet<Long>(parentMap.get(entity.getId()));
        log.debug("Got parents: {}", parentIds);

        entityCache.invalidate(entity.getId());
        workspaceCache.remove(entity.getId());
        ontologyRootCache.remove(entity.getId());

        // Reload the entity and stick it into the cache
        Entity canonicalEntity = null;
        try {
            canonicalEntity = putOrUpdate(entityFacade.getEntityById(entity.getId()));
            log.debug("Got new canonical entity: {}", EntityUtils.identify(canonicalEntity));
        }
        catch (Exception e) {
            log.error("Problem reloading invalidated entity: {}", EntityUtils.identify(entity), e);
            return;
        }

        // Replace existing parents' children
        for (Long parentId : parentIds) {
            Entity parent = entityCache.getIfPresent(parentId);
            if (parent != null) {
                boolean found = false;
                for (EntityData ed : parent.getEntityData()) {
                    if (ed.getChildEntity() != null && canonicalEntity.getId().equals(ed.getChildEntity().getId())) {
                        log.trace("putOrUpdate: Replacing child {} on existing parent {}", EntityUtils.identify(canonicalEntity), EntityUtils.identify(parent));
                        ed.setChildEntity(canonicalEntity);
                        found = true;
                    }
                }
                if (!found) {
                    log.debug("Entity not found in parent: {}", EntityUtils.identify(parent));
                }
            }
            else {
                log.debug("Parent entity not found: {}", parentId);
            }
        }

        if (recurse) {
            for (Entity child : entity.getChildren()) {
                invalidate(child, visited, true);
            }
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
            Entity entity = entityFacade.getEntityById(entityId);
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
        Entity entity = entityCache.getIfPresent(entityId);
        if (entity != null) {
            log.debug("getEntityById: returning cached entity {}", EntityUtils.identify(entity));
            return entity;
        }
        synchronized (this) {
            return putOrUpdate(entityFacade.getEntityById(entityId));
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

        Map<Long, Entity> entityMap = new HashMap<Long, Entity>();
        List<Long> unfound = new ArrayList<Long>(entityIds);

        for (Iterator<Long> i = unfound.iterator(); i.hasNext();) {
            Long entityId = i.next();
            Entity e = entityCache.getIfPresent(entityId);
            if (e != null) {
                entityMap.put(e.getId(), e);
                i.remove();
            }
        }

        if (!unfound.isEmpty()) {
            synchronized (this) {
                List<Entity> remaining = entityFacade.getEntitiesById(unfound);
                for (Entity entity : putOrUpdateAll(remaining)) {
                    entityMap.put(entity.getId(), entity);
                }
            }
        }

        // Order results in the way they were requested
        List<Entity> result = new ArrayList<Entity>();
        for (Long entityId : entityIds) {
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
        replaceUnloadedChildrenWithForbiddenEntities(entity, true, null);
        return putOrUpdate(entity, true);
    }

    /**
     * Retrieve an entity with its children loaded.
     *
     * @return
     */
    public Entity getEntityAndChildren(long entityId) throws Exception {
        synchronized (this) {
            Entity entity = getEntityById(entityId);
            if (entity == null) {
                return null;
            }
            if (!EntityUtils.areLoaded(entity.getEntityData())) {
                refreshChildren(entity);
            }
            return entity;
        }
    }

    /**
     * Retrieve the children of the given entity from the database, and update the cache.
     *
     * @param entity
     * @throws Exception
     */
    public void refreshChildren(Entity entity) throws Exception {
        checkIfCanonicalEntity(entity);
        log.debug("Refreshing children for: {}", EntityUtils.identify(entity));
        synchronized (this) {
            Set<Entity> childEntitySet = entityFacade.getChildEntities(entity.getId());
            putOrUpdateAll(childEntitySet);
            putOrUpdate(entity);
            replaceUnloadedChildrenWithForbiddenEntities(entity, false, null);
        }
        notifyEntityChildrenLoaded(entity);
    }

    /**
     * Retrieve the children (or ancestors) of the given entity from the database which are not loaded already, and update the cache.
     *
     * @param entity
     * @param recurse
     * @return canonical entity instance
     * @throws Exception
     */
    public Entity loadLazyEntity(Entity entity, boolean recurse) throws Exception {
        Entity retEntity = putOrUpdate(entity);
        log.debug((recurse ? "Recursively loading" : "Loading") + " lazy entity: {}", EntityUtils.identify(entity));
        if (recurse) {
            loadLazyEntity(retEntity, new HashSet<Long>());
        }
        else {
            if (!EntityUtils.areLoaded(retEntity.getEntityData())) {
                refreshChildren(retEntity);
            }
            else {
                log.debug("Entity is already loaded: {}", EntityUtils.identify(entity));
                notifyEntityChildrenLoaded(entity);
            }
        }
        return retEntity;
    }

    private void loadLazyEntity(Entity entity, Set<Long> visited) throws Exception {
        if (entity == null) {
            log.warn("Cannot load lazy entity when entity is null");
            return;
        }
        if (!EntityUtils.isInitialized(entity)) {
            return;
        }
        if (visited.contains(entity.getId())) {
            return;
        }
        if (entity instanceof ForbiddenEntity) {
            return;
        }
        visited.add(entity.getId());
        if (!EntityUtils.areLoaded(entity.getEntityData())) {
            refreshChildren(entity);
        }
        for (Entity child : entity.getChildren()) {
            loadLazyEntity(child, visited);
        }
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
     * Retrieve entities owned by the current user, with the given name from the database, and cache all the entities.
     *
     * @param entityName
     * @return canonical entity instances
     * @throws Exception
     */
    public List<Entity> getOwnedEntitiesByName(String entityName) throws Exception {
        List<Entity> entities = FacadeManager.getFacadeManager().getEntityFacade().getOwnedEntitiesByName(entityName);
        return putOrUpdateAll(entities);
    }

    /**
     * Create a new entity with the given type and name, and cache it.
     *
     * @param entityTypeName
     * @param entityName
     * @return canonical entity instance
     * @throws Exception
     */
    public Entity createEntity(String entityTypeName, String entityName) throws Exception {
        Entity entity = null;
        synchronized (this) {
            entity = entityFacade.createEntity(entityTypeName, entityName);
            entity = putOrUpdate(entity);
            log.debug("Created new entity: {}", EntityUtils.identify(entity));
        }
        notifyEntityCreated(entity);
        return entity;
    }

    /**
     * Rename the given entity, and update the cache.
     *
     * @param entity
     * @param newName
     * @return canonical entity instance
     * @throws Exception
     */
    public Entity renameEntity(Entity entity, String newName) throws Exception {
        checkIfCanonicalEntity(entity);
        Entity canonicalEntity = null;
        synchronized (this) {
            entity.setName(newName);
            canonicalEntity = putOrUpdate(entityFacade.saveEntity(entity));
            log.debug("Renamed entity: {}", EntityUtils.identify(entity));
        }
        return canonicalEntity;
    }

    /**
     * Set the given attribute as a tag (i.e. using the attribute name as the attribute value) on the specified entity, and update the cache.
     *
     * @param entity
     * @param attributeName
     * @return canonical entity instance
     * @throws Exception
     */
    public Entity setAttributeAsTag(Entity entity, String attributeName) throws Exception {
        checkIfCanonicalEntity(entity);
        Entity canonicalEntity = null;
        synchronized (this) {
            Entity newEntity = entityFacade.getEntityById(entity.getId());
            String value = newEntity.getValueByAttributeName(attributeName);
            if (value == null || !value.equals(attributeName)) {
                EntityUtils.addAttributeAsTag(newEntity, attributeName);
                canonicalEntity = putOrUpdate(entityFacade.saveEntity(newEntity));
            }
            else {
                canonicalEntity = getEntityById(entity.getId());
            }
        }
        return canonicalEntity;
    }

    /**
     * Generic save. Use of this method should be avoided whenever possible. However, it can be useful for bulk changes to entity data objects.
     *
     * @param entity
     * @return canonical entity instance
     * @throws Exception
     */
    public Entity saveEntity(Entity entity) throws Exception {
        checkIfCanonicalEntity(entity);
        Entity canonicalEntity = null;
        synchronized (this) {
            Set<Long> forbiddenIds = new HashSet<Long>();
            replaceForbiddenEntitiesWithUnloadedChildren(entity, true, forbiddenIds);
            Entity newEntity = entityFacade.saveEntity(entity);
            replaceUnloadedChildrenWithForbiddenEntities(newEntity, true, forbiddenIds);
            canonicalEntity = putOrUpdate(newEntity);
        }
        return canonicalEntity;
    }

    /**
     * Generic save. Use of this method should be avoided whenever possible.
     *
     * @param entityData
     * @return EntityData with canonical parent/child entity instances
     * @throws Exception
     */
    public EntityData saveEntityData(EntityData entityData) throws Exception {
        EntityData savedEd = entityFacade.saveEntityDataForEntity(entityData);

        Entity parent = savedEd.getParentEntity();
        if (parent != null) {
            Entity canonicalParent = entityCache.getIfPresent(parent.getId());
            if (canonicalParent != null) {
                savedEd.setParentEntity(canonicalParent);
                notifyEntityChanged(canonicalParent);
            }
        }

        Entity child = savedEd.getChildEntity();
        if (child != null) {
            Entity canonicalChild = entityCache.getIfPresent(child.getId());
            if (canonicalChild != null) {
                savedEd.setChildEntity(canonicalChild);
            }
        }

        return savedEd;
    }

    /**
     * Update the index of the given relationship within its parent.
     *
     * @param entityData
     * @return EntityData with canonical parent/child entity instances
     * @throws Exception
     */
    public EntityData updateChildIndex(EntityData entityData, Integer orderIndex) throws Exception {
        entityFacade.updateChildIndex(entityData, orderIndex);

        Entity parent = entityData.getParentEntity();
        if (parent != null) {
            Entity canonicalParent = entityCache.getIfPresent(parent.getId());
            if (canonicalParent != null) {
                entityData.setOrderIndex(orderIndex);
                notifyEntityChanged(canonicalParent);
            }
        }

        return entityData;
    }

    /**
     * Update the index of the given relationship within its parent.
     *
     * @param entityData
     * @return EntityData with canonical parent/child entity instances
     * @throws Exception
     */
    public Entity updateChildIndexes(Entity entity) throws Exception {
        checkIfCanonicalEntity(entity);
        entityFacade.updateChildIndexes(entity);
        notifyEntityChanged(entity);
        return entity;
    }

    /**
     * Update the value of the given attribute on the specified entity.
     *
     * @return EntityData with canonical parent/child entity instances
     * @throws Exception
     */
    public EntityData setOrUpdateValue(Entity entity, String attributeName, String value) throws Exception {
        checkIfCanonicalEntity(entity);
        EntityData savedEd = entityFacade.setOrUpdateValue(entity.getId(), attributeName, value);

        EntityData existingEd = entity.getEntityDataByAttributeName(attributeName);
        if (existingEd != null) {
            existingEd.setValue(value);
            notifyEntityChanged(entity);
        }
        else {
            invalidate(entity, false);
        }

        return savedEd;
    }

    /**
     * Update the value of the given attribute on the specified entities.
     *
     * @return collection EntityData with canonical parent/child entities instances
     * @throws Exception
     */
    public Collection<EntityData> setOrUpdateValues(Collection<Entity> entities, String attributeName, String value) throws Exception {
        // Convert to ids.  Check sanity.
        Collection<Long> entityIds = new ArrayList<Long>();
        for (Entity entity : entities) {
            checkIfCanonicalEntity(entity);
            entityIds.add(entity.getId());
        }

        Collection<EntityData> savedEds = entityFacade.setOrUpdateValues(entityIds, attributeName, value);

        // Carry out entity notifications.  Set value in cache.
        // Works against original entity list.
        for (Entity entity : entities) {
            EntityData existingEd = entity.getEntityDataByAttributeName(attributeName);
            if (existingEd != null) {
                notifyEntityChanged(entity);
                existingEd.setValue(value);
            }
            else {
                invalidate(entity, false);
            }
        }
        return savedEds;
    }

    /**
     * Delete the given EntityData object.
     *
     * @param entityData
     * @throws Exception
     */
    public void deleteEntityData(EntityData entityData) throws Exception {
        Entity parent = null;
        synchronized (this) {
            log.debug("Deleting entity data {}", entityData.getId());
            entityFacade.removeEntityData(entityData);
            if (entityData.getChildEntity() != null) {
                Entity child = entityCache.getIfPresent(entityData.getChildEntity().getId());
                if (child != null) {
                    invalidate(child, false);
                }
            }
            if (entityData.getParentEntity() != null) {
                parent = entityCache.getIfPresent(entityData.getParentEntity().getId());
                if (parent != null) {
                    invalidate(parent, false);
                }
            }
        }
    }

    /**
     * Delete all of the specified EntityData objects from the given Entity.
     *
     * This method generates an EntityChanged event.
     *
     * @param entityData
     * @throws Exception
     */
    public void deleteBulkEntityData(Entity parent, Collection<EntityData> toDelete) throws Exception {
        checkIfCanonicalEntity(parent);
        synchronized (this) {
            for (EntityData entityData : toDelete) {
                log.debug("Deleting entity data " + entityData.getId());
                entityFacade.removeEntityData(entityData);
                parent.getEntityData().remove(entityData);
            }
            invalidate(parent, false);
        }
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

        Set<EntityData> parentEds = new HashSet<EntityData>();

        synchronized (this) {
            log.debug("Deleting entity tree: {}", EntityUtils.identify(entity));
            entityFacade.deleteEntityTree(entity.getId());

            // Remove from cache
            entityCache.invalidate(entity.getId());
            workspaceCache.remove(entity.getId());
            ontologyRootCache.remove(entity.getId());

            // Go through the cache and evict any entities which have this entity as a child
            for (Entity parent : entityCache.asMap().values()) {
                boolean invalidate = false;
                for (Iterator<EntityData> edIterator = parent.getEntityData().iterator(); edIterator.hasNext();) {
                    EntityData childEd = edIterator.next();
                    if (childEd.getChildEntity() != null && childEd.getChildEntity().getId().equals(entity.getId())) {
                        log.debug("Found parent: {}", EntityUtils.identify(parent));
                        invalidate = true;
                        edIterator.remove();
                        parentEds.add(childEd);
                    }
                }
                if (invalidate) {
                    log.debug("Invalidating parent {}", EntityUtils.identify(parent));
                    invalidate(parent, false);
                }
            }
        }
        notifyEntityRemoved(entity, parentEds);
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
        return addEntityToParent(parent, entity, parent.getMaxOrderIndex() == null ? 0 : parent.getMaxOrderIndex() + 1, EntityConstants.ATTRIBUTE_ENTITY);
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
        synchronized (this) {
            ed = entityFacade.addEntityToParent(parent, entity, index, attrName);
            ed.setChildEntity(entity); // replace with a canonical entities
            putOrUpdate(ed.getParentEntity()); // update child count
            ed.setParentEntity(parent);
            parent.getEntityData().add(ed);
            log.debug("Added entity (id={}) to parent (id={})", entity.getId(), parent.getId());
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
        synchronized (this) {
            entityFacade.addChildren(parentId, childrenIds, attributeName);
        }
        notifyEntityChanged(reloadById(parentId));
    }

    /**
     * Create a new common root folder and cache it.
     *
     * @param folderName
     * @return canonical entity instance
     * @throws Exception
     */
    public Entity createCommonRootFolder(Long workspaceId, String folderName) throws Exception {
        Entity commonRoot = null;
        synchronized (this) {
            commonRoot = annotationFacade.createFolderInWorkspace(workspaceId, folderName);
            log.debug("Created new common root: {}", EntityUtils.identify(commonRoot));
            commonRoot = putOrUpdate(commonRoot);
            Collection<Long> ids = new ArrayList<Long>();
            ids.add(workspaceId);
            invalidate(ids);
        }
        return commonRoot;
    }

    /**
     * Create a new ontology root and cache it.
     *
     * @param ontologyName
     * @return canonical entity instance
     * @throws Exception
     */
    public Entity createOntologyRoot(String ontologyName) throws Exception {
        Entity newRoot = null;
        synchronized (this) {
            newRoot = ontologyFacade.createOntologyRoot(ontologyName);
            log.debug("Created new ontology root: {}", EntityUtils.identify(newRoot));
            newRoot = putOrUpdate(newRoot);
        }
        notifyEntityCreated(newRoot);
        return newRoot;
    }

    /**
     * Create a new ontology element and cache it.
     *
     * @param ontologyName
     * @return canonical entity instance
     * @throws Exception
     */
    public Entity createOntologyTerm(Long parentId, String label, OntologyElementType type, Integer orderIndex) throws Exception {
        Entity parent = getEntityById(parentId);
        EntityData ed = null;
        Entity newTerm = null;
        synchronized (this) {
            ed = ontologyFacade.createOntologyTerm(parentId, label, type, orderIndex);
            ed.setParentEntity(parent); // replace with a canonical entities
            parent.getEntityData().add(ed);
            newTerm = ed.getChildEntity();
            log.debug("Created new ontology term: {}", EntityUtils.identify(newTerm));
            newTerm = putOrUpdate(newTerm);
        }
        notifyEntityChanged(parent);
        return newTerm;
    }

    /**
     * Retrieve an existing ontology root folder by name.
     *
     * @param ontologyRoot
     * @return canonical entity instance
     */
    public Entity getOntologyRoot(String rootName) throws Exception {
        for (Entity ontologyRoot : getOntologyRoots()) {
            if (ontologyRoot.getName().equals(rootName) && ModelMgrUtils.isOwner(ontologyRoot)) {
                return ontologyRoot;
            }
        }
        return null;
    }

    /**
     * Demote a common root so that it becomes an ordinary folder.
     *
     * @param commonRoot
     * @throws Exception
     */
    public void demoteCommonRootToFolder(Entity commonRoot) throws Exception {
        checkIfCanonicalEntity(commonRoot);
        synchronized (this) {
            EntityData rootTagEd = commonRoot.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT);
            checkArgument(rootTagEd != null, "Entity is not a common root: {}", EntityUtils.identify(commonRoot));
            entityFacade.removeEntityData(rootTagEd);
            log.debug("Demoted common root {} to regular folder", EntityUtils.identify(commonRoot));
            commonRoot.getEntityData().remove(rootTagEd);
        }
        notifyEntityChanged(commonRoot);
    }

    /**
     * Returns all the common roots that the user has access to.
     *
     * @return canonical entity instances
     * @throws Exception
     */
    public List<Entity> getWorkspaces() throws Exception {
        synchronized (this) {
            if (workspaceCache.isEmpty()) {
                log.debug("Getting workspaces");
                for (Entity workspace : annotationFacade.getWorkspaces()) {
                    loadLazyEntity(workspace, false);
                    workspaceCache.put(workspace.getId(), workspace);
                }
            }
        }
        return new ArrayList<Entity>(workspaceCache.values());
    }

    /**
     * Returns all the roots in the given workspace with the specified name.
     *
     * @param workspaceId
     * @param name
     * @return
     * @throws Exception
     */
    public List<Entity> getCommonRootsByName(Long workspaceId, String name) throws Exception {
        List<Entity> results = new ArrayList<Entity>();
        Entity workspace = getEntityById(workspaceId);
        for (Entity commonRoot : workspace.getChildren()) {
            if (commonRoot.getName().equals(name)) {
                results.add(commonRoot);
            }
        }
        return results;
    }

    /**
     * Returns the first common root with a given name which is owned by the user.
     *
     * @param name
     * @return
     * @throws Exception
     */
    public Entity getOwnedCommonRootByName(String name) throws Exception {
        for (Entity workspace : getWorkspaces()) {
            for (Entity commonRoot : workspace.getOrderedChildren()) {
                if (ModelMgrUtils.isOwner(commonRoot) && commonRoot.getName().equals(name)) {
                    return commonRoot;
                }
            }
        }
        return null;
    }

    /**
     * Returns all the ontology roots that the user has access to.
     *
     * @return canonical entity instances
     * @throws Exception
     */
    public List<Entity> getOntologyRoots() throws Exception {
        synchronized (this) {
            if (ontologyRootCache.isEmpty()) {
                log.debug("Getting ontology roots");
                for (Entity ontologyRoot : ontologyFacade.getOntologyRootEntities()) {
                    Entity cachedRoot = putOrUpdate(ontologyRoot, true);
                    ontologyRootCache.put(cachedRoot.getId(), cachedRoot);

                }
            }
        }
        return new ArrayList<Entity>(ontologyRootCache.values());
    }

    /**
     * Returns all of the alignment spaces that the user has access to.
     *
     * @return canonical entity instances
     * @throws Exception
     */
    public List<Entity> getAlignmentSpaces() throws Exception {
        synchronized (this) {
            List<Entity> alignmentSpaces = entityFacade.getAlignmentSpaces();
            return putOrUpdateAll(alignmentSpaces);
        }
    }

    /**
     * Create a new Alignment board and cache it.
     *
     * @param boardName Name of the Alignment Board
     * @return canonical entity instance
     * @throws Exception
     */
    public RootedEntity createAlignmentBoard(Long workspaceId, String alignmentBoardName, String alignmentSpace, String opticalRes, String pixelRes) throws Exception {
        synchronized (this) {

            Entity boardEntity = annotationFacade.createAlignmentBoard(alignmentBoardName, alignmentSpace, opticalRes, pixelRes);
            Entity alignmentBoardFolder = getOwnedCommonRootByName(EntityConstants.NAME_ALIGNMENT_BOARDS);
            if (alignmentBoardFolder == null) {
                createCommonRootFolder(workspaceId, EntityConstants.NAME_ALIGNMENT_BOARDS);
            }

            RootedEntity abRootedEntity = new RootedEntity(alignmentBoardFolder);

            loadLazyEntity(alignmentBoardFolder, false);

            EntityData childEd = null;
            for (EntityData ed : alignmentBoardFolder.getEntityData()) {
                if (ed.getChildEntity() != null && ed.getChildEntity().getId().equals(boardEntity.getId())) {
                    childEd = ed;
                }
            }

            if (childEd == null) {
                throw new IllegalStateException("Could not retrieve the new alignment board");
            }

            return abRootedEntity.getChild(childEd);
        }
    }

    /**
     * Create a new Alignment board and cache it.
     *
     * @param boardName Name of the Alignment Board
     * @return canonical entity instance
     * @throws Exception
     */
    public RootedEntity addAlignedItem(Entity parent, Entity entity, String alignedItemName, boolean visible) throws Exception {

        checkIfCanonicalEntity(parent);
        checkIfCanonicalEntity(entity);

        synchronized (this) {
            EntityData alignedItemEntityEd = annotationFacade.addAlignedItem(parent, entity, alignedItemName, visible);
            Entity newAlignedEntity = alignedItemEntityEd.getChildEntity();
            putOrUpdate(newAlignedEntity);

            alignedItemEntityEd.setParentEntity(parent); // replace with a canonical entity
            parent.getEntityData().add(alignedItemEntityEd);
            log.debug("Added aligned item (id={}) to parent (id={})", entity.getId(), parent.getId());
            notifyEntityChanged(parent);

            RootedEntity abRootedEntity = new RootedEntity(getEntityById(parent.getId()));
            return abRootedEntity.getChildById(newAlignedEntity.getId());
        }
    }

    /**
     * Returns the first ancestor of the given type that it can find.
     *
     * @param entity
     * @param typeName
     * @return canonical entity instances
     * @throws Exception
     */
    public Entity getAncestorWithType(Entity entity, String typeName) throws Exception {
        Entity ancestor = entityFacade.getAncestorWithType(entity, typeName);
        return putOrUpdate(ancestor);
    }

    /**
     * Returns all of the parents of the given entity.
     *
     * @param childEntityId
     * @return canonical entity instances
     */
    public List<Entity> getParentEntities(Long childEntityId) throws Exception {
        return putOrUpdateAll(entityFacade.getParentEntities(childEntityId));
    }

    /**
     * Returns all of the entity datas providing links to the parents of the given entity.
     *
     * @param childEntityId
     * @return
     */
    public List<EntityData> getParentEntityDatas(Long childEntityId) throws Exception {
        List<EntityData> entities = new ArrayList<EntityData>();
        for (EntityData parentEd : entityFacade.getParentEntityDatas(childEntityId)) {
            Entity child = parentEd.getChildEntity();
            if (child != null) {
                parentEd.setChildEntity(getEntityById(child.getId()));
            }
            entities.add(parentEd);
        }
        return entities;
    }

    /**
     * Returns all of the entity datas providing links to the parents of the given entity.
     *
     * @param childEntityId
     * @return
     */
    public List<EntityData> getAllParentEntityDatas(Long childEntityId) throws Exception {
        List<EntityData> entities = new ArrayList<EntityData>();
        for (EntityData parentEd : entityFacade.getAllParentEntityDatas(childEntityId)) {
            Entity child = parentEd.getChildEntity();
            if (child != null) {
                parentEd.setChildEntity(getEntityById(child.getId()));
            }
            entities.add(parentEd);
        }
        return entities;
    }

    /**
     * Returns all of the entities of a given type. Be very careful calling this method, since certain types have many millions of members!
     *
     * @param entityTypeName
     * @return canonical entity instances
     */
    public List<Entity> getEntitiesByTypeName(String entityTypeName) throws Exception {
        return putOrUpdateAll(entityFacade.getEntitiesByTypeName(entityTypeName));
    }

    /**
     * Returns all of the entities of a given type, owned by the current user. Be very careful calling this method, since certain types have many
     * millions of members!
     *
     * @param entityTypeName
     * @return canonical entity instances
     */
    public List<Entity> getOwnedEntitiesByTypeName(String entityTypeName) throws Exception {
        return putOrUpdateAll(entityFacade.getOwnedEntitiesByTypeName(entityTypeName));
    }

    /**
     * Returns all data set entities for the current user.
     *
     * @return canonical entity instances
     * @throws Exception
     */
    public List<Entity> getDataSets() throws Exception {
        return putOrUpdateAll(annotationFacade.getDataSets());
    }

    /**
     * Create a new data set with the given name, for the current user.
     *
     * @param dataSetName
     * @return
     * @throws Exception
     */
    public Entity createDataSet(String dataSetName) throws Exception {
        return putOrUpdate(annotationFacade.createDataSet(dataSetName));
    }

    /**
     * Replace any uninitialized children with forbidden place-holders. The assumption is that we tried to load the children already, and the ones
     * that didn't come back must be inaccessible to the current user.
     *
     * @param parent
     */
    private void replaceUnloadedChildrenWithForbiddenEntities(Entity parent, boolean recurse, Set<Long> forbiddenIds) {
        if (parent == null) {
            return;
        }
        if (parent instanceof ForbiddenEntity) {
            return;
        }
        if (parent.getEntityData() == null) {
            return;
        }
        for (EntityData ed : parent.getEntityData()) {
            if (ed.getChildEntity() != null) {
                if (!EntityUtils.isInitialized(ed.getChildEntity())) {
                    if (forbiddenIds == null || forbiddenIds.contains(ed.getChildEntity().getId())) {
                        log.trace("Replacing unloaded entity with forbidden entity: {}", ed.getChildEntity().getId());
                        ed.setChildEntity(new ForbiddenEntity(ed.getChildEntity()));
                    }
                }
                else if (recurse) {
                    replaceUnloadedChildrenWithForbiddenEntities(ed.getChildEntity(), true, forbiddenIds);
                }
            }
        }
    }

    /**
     * Replace any forbidden place-holders with their original uninitialized children. This is useful if you want to send the entity tree back to the
     * server.
     *
     * @param parent
     */
    private void replaceForbiddenEntitiesWithUnloadedChildren(Entity parent, boolean recurse, Set<Long> forbiddenIds) {
        if (parent == null) {
            return;
        }
        if (!EntityUtils.isInitialized(parent)) {
            return;
        }
        if (parent.getEntityData() == null) {
            return;
        }
        for (EntityData ed : parent.getEntityData()) {
            if (ed.getChildEntity() instanceof ForbiddenEntity) {
                log.trace("Replacing forbidden entity with unloaded entity: {}", ed.getChildEntity().getId());
                ForbiddenEntity fe = (ForbiddenEntity) ed.getChildEntity();
                ed.setChildEntity(fe.getEntity());
                forbiddenIds.add(ed.getChildEntity().getId());
            }
            else if (recurse) {
                replaceForbiddenEntitiesWithUnloadedChildren(ed.getChildEntity(), true, forbiddenIds);
            }
        }
    }

    private void notifyEntityChildrenLoaded(Entity entity) {
        if (log.isTraceEnabled()) {
            log.trace("Generating EntityChildrenLoadedEvent for {}", EntityUtils.identify(entity));
        }
        ModelMgr.getModelMgr().postOnEventBus(new EntityChildrenLoadedEvent(entity));
    }

    private void notifyEntityCreated(Entity entity) {
        if (log.isTraceEnabled()) {
            log.trace("Generating EntityCreateEvent for {}", EntityUtils.identify(entity));
        }
        ModelMgr.getModelMgr().postOnEventBus(new EntityCreateEvent(entity));
    }

    private void notifyEntityChanged(Entity entity) {
        if (log.isTraceEnabled()) {
            log.trace("Generating EntityChangeEvent for {}", EntityUtils.identify(entity));
        }
        ModelMgr.getModelMgr().postOnEventBus(new EntityChangeEvent(entity));
    }

    private void notifyEntityRemoved(Entity entity, Set<EntityData> parentEds) {
        if (log.isTraceEnabled()) {
            log.trace("Generating EntityRemoveEvent for {}", EntityUtils.identify(entity));
        }
        ModelMgr.getModelMgr().postOnEventBus(new EntityRemoveEvent(entity, parentEds));
    }

    private void notifyEntitiesInvalidated(Collection<Entity> entities) {
        if (log.isTraceEnabled()) {
            log.trace("Generating EntityInvalidationEvent with {} entities", entities.size());
        }
        ModelMgr.getModelMgr().postOnEventBus(new EntityInvalidationEvent(entities));
    }
}
