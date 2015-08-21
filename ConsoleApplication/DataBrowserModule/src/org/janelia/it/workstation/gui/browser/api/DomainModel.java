package org.janelia.it.workstation.gui.browser.api;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.SwingUtilities;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.support.MongoUtils;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.DomainFacade;
import org.janelia.it.workstation.gui.browser.events.Events;
import org.janelia.it.workstation.gui.browser.events.model.DomainObjectAnnotationChangeEvent;
import org.janelia.it.workstation.gui.browser.events.model.DomainObjectChangeEvent;
import org.janelia.it.workstation.gui.browser.events.model.DomainObjectCreateEvent;
import org.janelia.it.workstation.gui.browser.events.model.DomainObjectInvalidationEvent;
import org.janelia.it.workstation.gui.browser.events.model.DomainObjectRemoveEvent;
import org.janelia.it.workstation.gui.browser.model.DomainObjectComparator;
import org.janelia.it.workstation.gui.browser.model.DomainObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a unified domain model for client-side operations. All changes to the model should go through this class, as well as any domain object accesses
 * which need to maintain the domain object in memory for more than one user interaction.
 *
 * A few guiding principles:
 *
 * 1) The methods in this class can potentially access the data source, so they should ALWAYS be called from a worker thread, never from the EDT. By
 * contrast, all resulting events are queued on the EDT, so that GUI widgets can immediately make use of the results.
 *
 * 2) Only one instance of any given domain object is ever maintained by this model. If the domain object has changed, the existing instance is updated with new
 * information. Duplicate instances are always discarded. The canonical instance (cached instance) is returned by most methods, and clients can
 * synchronize with the cache by using those instances.
 *
 * 3) Invalidating a given domain object removes it from the cache entirely. Clients are encouraged to listen for the corresponding DomainObjectInvalidationEvent,
 * and discard their references to invalidated entities.
 *
 * 4) Updates to the cache are atomic (synchronized to the DomainObjectModel instance).
 *
 * 5) Cached entities may expire at any time, if they are not referenced outside of the cache. Therefore, you should never write any code that relies
 * on entities being cached or uncached.
 *
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainModel {
    
    private static final Logger log = LoggerFactory.getLogger(DomainModel.class);
    
    private final DomainFacade facade;
    private final Cache<DomainObjectId, DomainObject> objectCache;
    private final Map<DomainObjectId, Workspace> workspaceCache;
    
    public DomainModel(DomainFacade facade) {
        this.facade = facade;
        this.objectCache = CacheBuilder.newBuilder().softValues().removalListener(new RemovalListener<DomainObjectId, DomainObject>() {
            @Override
            public void onRemoval(RemovalNotification<DomainObjectId, DomainObject> notification) {
                synchronized (DomainModel.this) {
                    DomainObjectId id = notification.getKey();
                    log.debug("removing {}",id);
                    if (workspaceCache.containsKey(id)) {
                        workspaceCache.clear();
                    }
                }
            }
        }).build();
        this.workspaceCache = new LinkedHashMap<>();
    }
    
    /**
     * Ensure the given object is in the cache, or generate appropriate warnings/exceptions.
     *
     * @param domainObject
     */
    private DomainObjectId checkIfCanonicalDomainObject(DomainObject domainObject) {
        DomainObjectId id = DomainObjectId.createFor(domainObject);
        try {
            DomainObject presentObject = objectCache.getIfPresent(id);
            if (presentObject == null) {
                throw new IllegalStateException("Not in domain object model: " + DomainUtils.identify(domainObject));
            }
            else if (presentObject != domainObject) {
                throw new IllegalStateException("DomainModel: Instance mismatch: " + domainObject.getName()
                        + " (cached=" + System.identityHashCode(presentObject)
                        + ") vs (this=" + System.identityHashCode(domainObject) + ")");
            }
        }
        catch (IllegalStateException e) {
            log.warn("Problem verifying domain model", e);
        }
        return id;
    }
    
    /**
     * Put the object in the cache, or update the cached domain object if there is already a version in the cache. In the latter case, the updated cached
     * instance is returned, and the argument instance can be discarded.
     *
     * If the domain object has children which are already cached, those references will be replaced with references to the cached instances.
     *
     * This method may generate an DomainObjectUpdated event.
     *
     * @param domainObject
     * @return canonical domain object instance
     */
    private <T extends DomainObject> T putOrUpdate(T domainObject) {
        if (domainObject == null) {
            // This is a null object, which cannot go into the cache
            log.trace("putOrUpdate: object is null");
            return null;
        }
        synchronized (this) {
            DomainObjectId id = DomainObjectId.createFor(domainObject);
            DomainObject canonicalObject = objectCache.getIfPresent(id);
            if (canonicalObject != null) {
                if (canonicalObject!=domainObject) {
                    log.debug("putOrUpdate: Updating cached instance: {} {}",id,DomainUtils.identify(canonicalObject));
                    objectCache.put(id, domainObject);
                    notifyDomainObjectsInvalidated(canonicalObject);
                }
                else {
                    log.debug("putOrUpdate: Returning cached instance: {} {}",id,DomainUtils.identify(canonicalObject));
                }
            }
            else {
                canonicalObject = domainObject;
                log.debug("putOrUpdate: Caching: {} {}",id,DomainUtils.identify(canonicalObject));
                objectCache.put(id, domainObject);
            }
            
            return (T)canonicalObject;
        }
    }
    
    /**
     * Call putOrUpdate on all the objects in a given collection.
     *
     * @param objects
     * @return canonical domain object instances
     */
    private List<DomainObject> putOrUpdateAll(Collection<DomainObject> objects) {
        List<DomainObject> putObjects = new ArrayList<>();
        for (DomainObject domainObject : objects) {
            putObjects.add(putOrUpdate(domainObject));
        }
        return putObjects;
    }
    
    /**
     * Clear the entire cache without raising any events. This is basically only useful for changing logins.
     */
    public void invalidateAll() {
        log.debug("Invalidating all objects");
        objectCache.invalidateAll();
        workspaceCache.clear();
        Events.getInstance().postOnEventBus(new DomainObjectInvalidationEvent());
    }

    /**
     * Invalidate the domain object in the cache, so that it will be reloaded on the next request.
     *
     * @param domainObject
     * @param recurse
     */
    public void invalidate(DomainObject domainObject) {
        List<DomainObject> objects = new ArrayList<>();
        objects.add(domainObject);
        invalidate(objects);
    }

    /**
     * Invalidate any number of entities in the cache, so that they will be reloaded on the next request.
     *
     * @param objects
     * @param recurse
     */
    private void invalidate(Collection<DomainObject> objects) {
        log.debug("Invalidating {} objects", objects.size());
        for (DomainObject domainObject : objects) {
            invalidateInternal(domainObject);
        }
        notifyDomainObjectsInvalidated(objects);
    }

    private void invalidateInternal(DomainObject domainObject) {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("DomainModel illegally called from EDT!");
        }
        DomainObjectId id = checkIfCanonicalDomainObject(domainObject);
        log.debug("Invalidating cached instance {}", DomainUtils.identify(domainObject));

        objectCache.invalidate(id);
        workspaceCache.remove(id);

        // Reload the domain object and stick it into the cache
        DomainObject canonicalDomainObject;
        try {
            canonicalDomainObject = loadDomainObject(id);
            if (canonicalDomainObject==null) {
                log.warn("Object no longer exists: "+id);
                return;
            }
            canonicalDomainObject = putOrUpdate(canonicalDomainObject);
            log.debug("Got new canonical object: {}", DomainUtils.identify(canonicalDomainObject));
        }
        catch (Exception e) {
            log.error("Problem reloading invalidated object: {}", DomainUtils.identify(domainObject), e);
        }
    }

    /**
     * Reload the given domain object from the database, and update the cache.
     *
     * @param domainObject
     * @return canonical domain object instance
     * @throws Exception
     */
    public DomainObject reload(DomainObject domainObject) throws Exception {
        return reloadById(DomainObjectId.createFor(domainObject));
    }

    /**
     * Reload all the entities in the given collection, and update the cache.
     *
     * @param objects
     * @return canonical domain object instances
     * @throws Exception
     */
    public List<DomainObject> reload(Collection<DomainObject> objects) throws Exception {
        return reloadById(DomainUtils.getDomainObjectIdList(objects));
    }

    /**
     * Reload the given domain object from the database, and update the cache.
     *
     * @param domainObjectId
     * @return canonical domain object instance
     * @throws Exception
     */
    public DomainObject reloadById(DomainObjectId domainObjectId) throws Exception {
        synchronized (this) {
            DomainObject domainobject = loadDomainObject(domainObjectId);
            return putOrUpdate(domainobject);
        }
    }

    /**
     * Reload all the entities in the given collection, and update the cache.
     *
     * @param domainObjectId
     * @return canonical domain object instances
     * @throws Exception
     */
    public List<DomainObject> reloadById(List<DomainObjectId> domainObjectIds) throws Exception {
        synchronized (this) {
            List<DomainObject> objects = loadDomainObjects(domainObjectIds);
            return putOrUpdateAll(objects);
        }
    }
    
    private DomainObject loadDomainObject(DomainObjectId id) {
        log.debug("loadDomainObject({})",id);
        Class<?> clazz;
        try {
            clazz = Class.forName(id.getClassName());
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException("Illegal domain object class: "+id.getClassName());
        }
        Reference ref = new Reference(MongoUtils.getCollectionName(clazz), id.getId());
        return facade.getDomainObject(ref);
    }
    
    private List<DomainObject> loadDomainObjects(Collection<DomainObjectId> ids) {
        List<DomainObject> objs = new ArrayList<>();
        for(DomainObjectId id : ids) {
            DomainObject domainObject = loadDomainObject(id);
            if (domainObject!=null) objs.add(domainObject);
        }
        return objs;
    }

    /**
     * Retrieve the given domain object, checking the cache first, and then falling back to the database.
     *
     * @param id
     * @return canonical domain object instance
     * @throws Exception
     */
    public DomainObject getDomainObjectByDomainObjectId(DomainObjectId id) throws Exception {
        log.info("Looking for {}",id);
        // This is sort of a hack to allow users to get objects by their superclass. 
        // TODO: Maybe instead of doing all this extra work, we could just key the cache by GUID. 
        for(Class<?> clazz : MongoUtils.getObjectClasses(MongoUtils.getObjectClassByName(id.getClassName()))) {
            DomainObjectId did = new DomainObjectId(clazz, id.getId());
            DomainObject domainObject = objectCache.getIfPresent(did);
            if (domainObject != null) {
                log.debug("getEntityById: returning cached domain object {}", DomainUtils.identify(domainObject));
                return domainObject;
            }
        }
        
        synchronized (this) {
            return putOrUpdate(loadDomainObject(id));
        }
    }
    
    public TreeNode getDomainObject(Class<? extends DomainObject> domainClass, Long id) throws Exception {
        return (TreeNode)getDomainObjectByDomainObjectId(new DomainObjectId(domainClass, id));
    }
    
    public List<DomainObject> getDomainObjectsByReference(List<Reference> references) {
                
        if (references==null) return new ArrayList<>();
        
        log.debug("getDomainObjects(references.size={})",references.size());
        
        Map<DomainObjectId,DomainObject> map = new HashMap<>();
        List<Reference> unsatisfiedRefs = new ArrayList<>();
        
        for(Reference ref : references) {
            DomainObjectId did = DomainUtils.getIdForReference(ref);
            DomainObject domainObject = did==null?null:objectCache.getIfPresent(did);
            if (domainObject!=null) {
                map.put(did, domainObject);
            }
            else {
                unsatisfiedRefs.add(ref);
            }
        }
        
        if (!unsatisfiedRefs.isEmpty()) {
            List<DomainObject> objects = facade.getDomainObjects(unsatisfiedRefs);
            map.putAll(DomainUtils.getMapByDomainObjectId(objects));
        }
        
        unsatisfiedRefs.clear();
        
        List<DomainObject> domainObjects = new ArrayList<>();
        for(Reference ref : references) {
            DomainObjectId did = DomainUtils.getIdForReference(ref);
            DomainObject domainObject = map.get(did);
            if (domainObject!=null) {
                domainObjects.add(domainObject);
            }
            else {
                unsatisfiedRefs.add(ref);
            }
        }
        
        log.debug("getDomainObjects: returning {} objects ({} unsatisfied)",domainObjects.size(),unsatisfiedRefs.size());
        return domainObjects;
    }
    
    public List<DomainObject> getDomainObjectsByDomainObjectId(List<DomainObjectId> domainObjectIds) {
        List<Reference> references = new ArrayList<>();
        for(DomainObjectId id : domainObjectIds) {
            references.add(DomainUtils.getReferenceForId(id));
        }
        return getDomainObjectsByReference(references);
    }
    
    public DomainObject getDomainObjectByReference(Reference reference) {
        return getDomainObject(reference.getTargetType(), reference.getTargetId());
    }
    
    public DomainObject getDomainObject(String type, Long id) {
        List<Long> ids = new ArrayList<>();
        ids.add(id);
        List<DomainObject> objects = getDomainObjects(type, ids);
        if (objects.isEmpty()) return null;
        return objects.get(0);
    }
    
    public List<DomainObject> getDomainObjects(String type, List<Long> ids) {
        
        if (ids==null) return new ArrayList<>();
        
        log.debug("getDomainObjects(ids.size={})",ids.size());
        
        Map<DomainObjectId,DomainObject> map = new HashMap<>();
        List<Long> unsatisfiedIds = new ArrayList<>();
        
        for(Long id : ids) {
            Reference ref = new Reference(type, id);
            DomainObjectId did = DomainUtils.getIdForReference(ref);
            DomainObject domainObject = did==null?null:objectCache.getIfPresent(did);
            if (domainObject!=null) {
                map.put(did, domainObject);
            }
            else {
                unsatisfiedIds.add(id);
            }
        }
        
        if (!unsatisfiedIds.isEmpty()) {
            List<DomainObject> objects = facade.getDomainObjects(type, unsatisfiedIds);
            map.putAll(DomainUtils.getMapByDomainObjectId(objects));
        }
        
        unsatisfiedIds.clear();
        
        List<DomainObject> domainObjects = new ArrayList<>();
        for(Long id : ids) {
            Reference ref = new Reference(type, id);
            DomainObjectId did = DomainUtils.getIdForReference(ref);
            DomainObject domainObject = map.get(did);
            if (domainObject!=null) {
                domainObjects.add(domainObject);
            }
            else {
                unsatisfiedIds.add(id);
            }
        }
        
        log.debug("getDomainObjects: returning {} objects ({} unsatisfied)",domainObjects.size(),unsatisfiedIds.size());
        return domainObjects;
    }
    
    public List<Annotation> getAnnotations(Long targetId) {
        // TODO: cache these?
        return facade.getAnnotations(DomainUtils.getCollectionOfOne(targetId));
    }
    
    public List<Annotation> getAnnotations(Collection<Long> targetIds) {
        
        if (targetIds==null) return new ArrayList<>();
        
        // TODO: cache these?
        return facade.getAnnotations(targetIds);
    }
    
    public Collection<Workspace> getWorkspaces() {
        synchronized (this) {
            if (workspaceCache.isEmpty()) {
                log.debug("Getting workspaces from database");
                for (Workspace workspace : facade.getWorkspaces()) {
                    Workspace cachedRoot = (Workspace)putOrUpdate(workspace);
                    if (cachedRoot instanceof Workspace) {
                        workspaceCache.put(DomainObjectId.createFor(cachedRoot), cachedRoot);
                    }
                }
            }
        }
        List<Workspace> workspaces = new ArrayList<>(workspaceCache.values());
        Collections.sort(workspaces, new DomainObjectComparator());
        return workspaces;
        
    }
    
    public Workspace getDefaultWorkspace() throws Exception {
        return getWorkspaces().iterator().next();
    }
    
    public Collection<Ontology> getOntologies() throws Exception {
        List<Ontology> ontologies = new ArrayList<>();
        for (Ontology ontology : facade.getOntologies()) {
            putOrUpdate(ontology);
            ontologies.add(ontology);
        }
        Collections.sort(ontologies, new DomainObjectComparator());
        return ontologies;
    }
    
    public void changePermissions(DomainObject domainObject, String granteeKey, String rights, boolean grant) throws Exception {
        synchronized (this) {
            ObjectSet objectSet = new ObjectSet();
            String type = MongoUtils.getCollectionName(domainObject.getClass());
            objectSet.setTargetType(type);
            objectSet.addMember(domainObject.getId());
            changePermissions(objectSet, granteeKey, rights, grant);
        }
    }
    
    public void changePermissions(ObjectSet objectSet, String granteeKey, String rights, boolean grant) throws Exception {
        synchronized (this) {
            facade.changePermissions(objectSet, granteeKey, rights, grant);
            for(Long id : objectSet.getMembers()) {
                putOrUpdate(getDomainObject(objectSet.getTargetType(), id));
            }
        }
    }
    
    public TreeNode create(TreeNode treeNode) throws Exception {
        TreeNode canonicalObject;
        synchronized (this) {
            canonicalObject = (TreeNode)putOrUpdate(facade.create(treeNode));
        }
        notifyDomainObjectCreated(canonicalObject);
        return canonicalObject;
    }
    
    public Filter save(Filter filter) throws Exception {
        Filter canonicalObject;
        synchronized (this) {
            canonicalObject = (Filter)putOrUpdate(filter.getId()==null ? facade.create(filter) : facade.update(filter));
        }
        notifyDomainObjectCreated(canonicalObject);
        return canonicalObject;
    }
    
    public ObjectSet create(ObjectSet objectSet) throws Exception {
        ObjectSet canonicalObject;
        synchronized (this) {
            canonicalObject = (ObjectSet)putOrUpdate(facade.create(objectSet));
        }
        notifyDomainObjectCreated(canonicalObject);
        return canonicalObject;
    }
    
    public Annotation create(Annotation annotation) throws Exception {
        Annotation canonicalObject;
        synchronized (this) {
            canonicalObject = (Annotation)putOrUpdate(facade.create(annotation));
        }
        notifyDomainObjectChanged(getDomainObjectByReference(annotation.getTarget()));
        return canonicalObject;
    }
    
    public void remove(Annotation annotation) throws Exception {
        facade.remove(annotation);
        notifyAnnotationsChanged(getDomainObjectByReference(annotation.getTarget()));
    }
    
    public void reorderChildren(TreeNode treeNode, int[] order) throws Exception {
        synchronized (this) {
            putOrUpdate(facade.reorderChildren(treeNode, order));
        }
        notifyDomainObjectChanged(treeNode);
    }
    
    public void addChild(TreeNode treeNode, DomainObject domainObject) throws Exception {
        synchronized (this) {
            putOrUpdate(facade.addChildren(treeNode, DomainUtils.getReferences(DomainUtils.getCollectionOfOne(domainObject))));
        }
        notifyDomainObjectChanged(treeNode);
    }
    
    public void addChildren(TreeNode treeNode, Collection<DomainObject> domainObjects) throws Exception {
        synchronized (this) {
            putOrUpdate(facade.addChildren(treeNode, DomainUtils.getReferences(domainObjects)));
        }
        notifyDomainObjectChanged(treeNode);
    }
    
    public void removeChild(TreeNode treeNode, DomainObject domainObject) throws Exception {
        synchronized (this) {
            putOrUpdate(facade.removeChildren(treeNode, DomainUtils.getReferences(DomainUtils.getCollectionOfOne(domainObject))));
        }
        notifyDomainObjectChanged(treeNode);
    }
    
    public void removeChildren(TreeNode treeNode, Collection<DomainObject> domainObjects) throws Exception {
        synchronized (this) {
            putOrUpdate(facade.removeChildren(treeNode, DomainUtils.getReferences(domainObjects)));
        }
        notifyDomainObjectChanged(treeNode);
    }
    
    public void removeReference(TreeNode treeNode, Reference reference) throws Exception {
        synchronized (this) {
            putOrUpdate(facade.removeChildren(treeNode, DomainUtils.getCollectionOfOne(reference)));
        }
    }
    
    public void addMember(ObjectSet objectSet, DomainObject domainObject) throws Exception {
        synchronized (this) {
            putOrUpdate(facade.addMembers(objectSet, DomainUtils.getReferences(DomainUtils.getCollectionOfOne(domainObject))));
        }
        notifyDomainObjectChanged(objectSet);
    }
    
    public void addMembers(ObjectSet objectSet, Collection<DomainObject> domainObjects) throws Exception {
        synchronized (this) {
            putOrUpdate(facade.addMembers(objectSet, DomainUtils.getReferences(domainObjects)));
        }
        notifyDomainObjectChanged(objectSet);
    }
    
    public void removeMember(ObjectSet objectSet, DomainObject domainObject) throws Exception {
        synchronized (this) {
            putOrUpdate(facade.removeMembers(objectSet, DomainUtils.getReferences(DomainUtils.getCollectionOfOne(domainObject))));
        }
    }
    
    public void removeMembers(ObjectSet objectSet, Collection<DomainObject> domainObjects) throws Exception {
        synchronized (this) {
            putOrUpdate(facade.removeMembers(objectSet, DomainUtils.getReferences(domainObjects)));
        }
        notifyDomainObjectChanged(objectSet);
    }
    
    public void updateProperty(DomainObject domainObject, String propName, String propValue) {
        synchronized (this) {
            putOrUpdate(facade.updateProperty(domainObject, propName, propValue));
        }
        notifyDomainObjectChanged(domainObject);
    }
    
    private void notifyDomainObjectCreated(DomainObject domainObject) {
        if (log.isTraceEnabled()) {
            log.trace("Generating DomainObjectCreateEvent for {}", DomainUtils.identify(domainObject));
        }
        Events.getInstance().postOnEventBus(new DomainObjectCreateEvent(domainObject));
    }

    private void notifyAnnotationsChanged(DomainObject domainObject) {
        if (log.isTraceEnabled()) {
            log.trace("Generating DomainObjectAnnotationChangeEvent for {}", DomainUtils.identify(domainObject));
        }
        Events.getInstance().postOnEventBus(new DomainObjectAnnotationChangeEvent(domainObject));
    }
    
    private void notifyDomainObjectChanged(DomainObject domainObject) {
        if (log.isTraceEnabled()) {
            log.trace("Generating DomainObjectChangeEvent for {}", DomainUtils.identify(domainObject));
        }
        Events.getInstance().postOnEventBus(new DomainObjectChangeEvent(domainObject));
    }

    private void notifyDomainObjectRemoved(DomainObject domainObject) {
        if (log.isTraceEnabled()) {
            log.trace("Generating DomainObjectRemoveEvent for {}", DomainUtils.identify(domainObject));
        }
        Events.getInstance().postOnEventBus(new DomainObjectRemoveEvent(domainObject));
    }

    private void notifyDomainObjectsInvalidated(Collection<DomainObject> objects) {
        if (log.isTraceEnabled()) {
            log.trace("Generating DomainObjectInvalidationEvent with {} entities", objects.size());
        }
        Events.getInstance().postOnEventBus(new DomainObjectInvalidationEvent(objects));
    }

    private void notifyDomainObjectsInvalidated(DomainObject domainObject) {
        if (log.isTraceEnabled()) {
            log.trace("Generating EntityInvalidationEvent for {}", DomainUtils.identify(domainObject));
        }
        Collection<DomainObject> invalidated = new ArrayList<>();
        invalidated.add(domainObject);
        Events.getInstance().postOnEventBus(new DomainObjectInvalidationEvent(invalidated));
    }
}
