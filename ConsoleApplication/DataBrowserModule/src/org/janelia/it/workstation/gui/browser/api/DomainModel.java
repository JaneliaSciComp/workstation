package org.janelia.it.workstation.gui.browser.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ReverseReference;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.domain.ontology.OntologyTermReference;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

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
    private final Cache<Reference, DomainObject> objectCache;
    private final Map<Reference, Workspace> workspaceCache;
    
    public DomainModel(DomainFacade facade) {
        this.facade = facade;
        this.objectCache = CacheBuilder.newBuilder().softValues().removalListener(new RemovalListener<Reference, DomainObject>() {
            @Override
            public void onRemoval(RemovalNotification<Reference, DomainObject> notification) {
                synchronized (DomainModel.this) {
                    Reference id = notification.getKey();
                    log.trace("removed key from cache: {}",id);
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
    private Reference checkIfCanonicalDomainObject(DomainObject domainObject) {
        Reference id = Reference.createFor(domainObject);
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
     * @param domainObject
     * @return canonical domain object instance
     */
    private <T extends DomainObject> T putOrUpdate(T domainObject) {
        if (domainObject == null) {
            // This is a null object, which cannot go into the cache
            log.debug("putOrUpdate: object is null");
            return null;
        }
        synchronized (this) {
            Reference id = Reference.createFor(domainObject);
            DomainObject canonicalObject = objectCache.getIfPresent(id);
            if (canonicalObject != null) {
                if (canonicalObject!=domainObject) {
                    canonicalObject = domainObject;
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
        Reference id = checkIfCanonicalDomainObject(domainObject);
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
     * Load an object from the database. 
     * @param id
     * @return
     */
    private DomainObject loadDomainObject(Reference ref) {
        log.debug("loadDomainObject({})",ref);
        return facade.getDomainObject(ref);
    }
    
    /**
     * Reload the given domain object from the database, and update the cache.
     *
     * @param domainObject
     * @return canonical domain object instance
     * @throws Exception
     */
    public DomainObject reload(DomainObject domainObject) throws Exception {
        return reloadById(Reference.createFor(domainObject));
    }

    /**
     * Reload the given domain object from the database, and update the cache.
     *
     * @param Reference
     * @return canonical domain object instance
     * @throws Exception
     */
    public DomainObject reloadById(Reference Reference) throws Exception {
        synchronized (this) {
            DomainObject domainobject = loadDomainObject(Reference);
            return putOrUpdate(domainobject);
        }
    }

    /**
     * Reload all the entities in the given collection, and update the cache.
     *
     * @param objects
     * @return canonical domain object instances
     * @throws Exception
     */
    public List<DomainObject> reload(Collection<DomainObject> objects) throws Exception {
        return reloadById(DomainUtils.getReferences(objects));
    }

    /**
     * Reload all the entities in the given collection, and update the cache.
     *
     * @param Reference
     * @return canonical domain object instances
     * @throws Exception
     */
    public List<DomainObject> reloadById(Collection<Reference> refs) throws Exception {
        synchronized (this) {
            List<DomainObject> objs = new ArrayList<>();
            for(Reference id : refs) {
                DomainObject domainObject = loadDomainObject(id);
                if (domainObject!=null) objs.add(domainObject);
            }
            return putOrUpdateAll(objs);
        }
    }

    /**
     * Retrieve the given domain object, checking the cache first and then the database.
     * @param domainObject
     * @return canonical domain object instance
     */
    public DomainObject getDomainObject(DomainObject domainObject) {
        return getDomainObject(Reference.createFor(domainObject));
    }
    
    /**
     * Retrieve the given domain object, checking the cache first and then the database.
     * @param ref
     * @return canonical domain object instance
     * @throws Exception
     */
    public DomainObject getDomainObject(Reference ref) {
        log.debug("getDomainObjectByReference({})",ref);
        DomainObject domainObject = objectCache.getIfPresent(ref);
        if (domainObject != null) {
            log.debug("getEntityById: returning cached domain object {}", DomainUtils.identify(domainObject));
            return domainObject;
        }
        synchronized (this) {
            return putOrUpdate(loadDomainObject(ref));
        }
    }

    public List<DomainObject> getDomainObjects(List<Reference> references) {
                
        if (references==null) return new ArrayList<>();
        
        log.debug("getDomainObjects(references.size={})",references.size());
        
        Map<Reference,DomainObject> map = new HashMap<>();
        List<Reference> unsatisfiedRefs = new ArrayList<>();
        
        for(Reference ref : references) {
            DomainObject domainObject = ref==null?null:objectCache.getIfPresent(ref);
            if (domainObject!=null) {
                map.put(ref, domainObject);
            }
            else {
                unsatisfiedRefs.add(ref);
            }
        }
        
        if (!unsatisfiedRefs.isEmpty()) {
            List<DomainObject> objects = facade.getDomainObjects(unsatisfiedRefs);
            map.putAll(DomainUtils.getMapByReference(objects));
        }
        
        unsatisfiedRefs.clear();
        
        List<DomainObject> domainObjects = new ArrayList<>();
        for(Reference ref : references) {
            DomainObject domainObject = map.get(ref);
            if (domainObject!=null) {
                domainObjects.add(putOrUpdate(domainObject));
            }
            else {
                unsatisfiedRefs.add(ref);
            }
        }
        
        log.debug("getDomainObjectsByReference: returning {} objects ({} unsatisfied)",domainObjects.size(),unsatisfiedRefs.size());
        return domainObjects;
    }

    public <T extends DomainObject> T getDomainObject(Class<T> domainClass, Long id) {
        List<T> list = getDomainObjects(domainClass, Arrays.asList(id));
        return list.isEmpty() ? null : list.get(0);
    }
    
    public <T extends DomainObject> List<T> getDomainObjects(Class<T> clazz, List<Long> ids) {
        List<T> objects = new ArrayList<>();
        for(DomainObject domainObject : getDomainObjects(clazz.getName(), ids)) {
            objects.add((T)domainObject);
        }
        return objects;
    }

    public DomainObject getDomainObject(String className, Long id) {
        List<DomainObject> objects = getDomainObjects(className, Arrays.asList(id));
        return objects.isEmpty() ? null : objects.get(0);
    }
    
    public List<DomainObject> getDomainObjects(String className, List<Long> ids) {
        
        if (className==null || ids==null) return new ArrayList<>();
        
        log.debug("getDomainObjects(ids.size={})",ids.size());
        
        Map<Reference,DomainObject> map = new HashMap<>();
        List<Long> unsatisfiedIds = new ArrayList<>();
        
        for(Long id : ids) {
            Reference ref = new Reference(className, id);
            DomainObject domainObject = ref==null?null:objectCache.getIfPresent(ref);
            if (domainObject!=null) {
                map.put(ref, domainObject);
            }
            else {
                unsatisfiedIds.add(id);
            }
        }
        
        if (!unsatisfiedIds.isEmpty()) {
            List<DomainObject> objects = facade.getDomainObjects(className, unsatisfiedIds);
            map.putAll(DomainUtils.getMapByReference(objects));
        }
        
        unsatisfiedIds.clear();
        
        List<DomainObject> domainObjects = new ArrayList<>();
        for(Long id : ids) {
            Reference ref = new Reference(className, id);
            DomainObject domainObject = map.get(ref);
            if (domainObject!=null) {
                domainObjects.add(putOrUpdate(domainObject));
            }
            else {
                unsatisfiedIds.add(id);
            }
        }
        
        log.debug("getDomainObjects: returning {} objects ({} unsatisfied)",domainObjects.size(),unsatisfiedIds.size());
        return domainObjects;
    }


    public List<DomainObject> getDomainObjects(ReverseReference reverseReference) {
        // TODO: cache these?
        return facade.getDomainObjects(reverseReference);
    }
    
    public List<Annotation> getAnnotations(Reference reference) {
        // TODO: cache these?
        return facade.getAnnotations(Arrays.asList(reference));
    }
    
    public List<Annotation> getAnnotations(Collection<Reference> references) {
        if (references==null) return new ArrayList<>();
        // TODO: cache these?
        return facade.getAnnotations(references);
    }
    
    public Collection<Workspace> getWorkspaces() {
        synchronized (this) {
            if (workspaceCache.isEmpty()) {
                log.debug("Getting workspaces from database");
                for (Workspace workspace : facade.getWorkspaces()) {
                    Workspace cachedRoot = putOrUpdate(workspace);
                    workspaceCache.put(Reference.createFor(cachedRoot), cachedRoot);
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
            ontologies.add(putOrUpdate(ontology));
        }
        Collections.sort(ontologies, new DomainObjectComparator());
        return ontologies;
    }
    
    public OntologyTerm getOntologyTermByReference(OntologyTermReference reference) {
        Ontology ontology = getDomainObject(Ontology.class, reference.getOntologyId());
        return findTerm(ontology, reference.getOntologyTermId());
    }
    
    private OntologyTerm findTerm(OntologyTerm term, Long termId) {
        if (term.getId().equals(termId)) {
            return term;
        }
        if (term.getTerms()!=null) {
            for(OntologyTerm child : term.getTerms()) {
                OntologyTerm found = findTerm(child, termId);
                if (found!=null) {
                    return found;
                }
            }
        }
        return null;
    }

    public Ontology create(Ontology ontology) throws Exception {
        Ontology canonicalObject;
        synchronized (this) {
            canonicalObject = putOrUpdate(facade.create(ontology));
        }
        notifyDomainObjectCreated(canonicalObject);
        return canonicalObject;
    }

    public Ontology reorderOntologyTerms(Long ontologyId, Long parentTermId, int[] order) throws Exception {
        Ontology canonicalObject;
        synchronized (this) {
            canonicalObject = putOrUpdate(facade.reorderTerms(ontologyId, parentTermId, order));
        }
        notifyDomainObjectChanged(canonicalObject);
        return canonicalObject;
    }

    public Ontology addOntologyTerm(Long ontologyId, Long parentTermId, OntologyTerm term) throws Exception {
        return addOntologyTerms(ontologyId, parentTermId, Arrays.asList(term));
    }
    
    public Ontology addOntologyTerms(Long ontologyId, Long parentTermId, Collection<OntologyTerm> terms) throws Exception {
        return addOntologyTerms(ontologyId, parentTermId, terms, null);
    }
    
    public Ontology addOntologyTerms(Long ontologyId, Long parentTermId, Collection<OntologyTerm> terms, Integer index) throws Exception {
        Ontology canonicalObject;
        synchronized (this) {
            canonicalObject = putOrUpdate(facade.addTerms(ontologyId, parentTermId, terms, index));
        }
        notifyDomainObjectChanged(canonicalObject);
        return canonicalObject;
    }

    public Ontology removeOntologyTerm(Long ontologyId, Long parentTermId, Long termId) throws Exception {
        Ontology canonicalObject;
        synchronized (this) {
            canonicalObject = putOrUpdate(facade.removeTerm(ontologyId, parentTermId, termId));
        }
        notifyDomainObjectChanged(canonicalObject);
        return canonicalObject;
    }
    
    public void removeOntology(Long ontologyId) throws Exception {
        Ontology canonicalObject = getDomainObject(Ontology.class, ontologyId);
        synchronized (this) {
            facade.removeOntology(ontologyId);
        }
        notifyDomainObjectRemoved(canonicalObject);
    }
    
    public TreeNode create(TreeNode treeNode) throws Exception {
        TreeNode canonicalObject;
        synchronized (this) {
            canonicalObject = putOrUpdate(facade.create(treeNode));
        }
        notifyDomainObjectCreated(canonicalObject);
        return canonicalObject;
    }
    
    public Filter save(Filter filter) throws Exception {
        Filter canonicalObject;
        synchronized (this) {
            canonicalObject = putOrUpdate(filter.getId()==null ? facade.create(filter) : facade.update(filter));
        }
        notifyDomainObjectCreated(canonicalObject);
        return canonicalObject;
    }
    
    public ObjectSet create(ObjectSet objectSet) throws Exception {
        ObjectSet canonicalObject;
        synchronized (this) {
            canonicalObject = putOrUpdate(facade.create(objectSet));
        }
        notifyDomainObjectCreated(canonicalObject);
        return canonicalObject;
    }
    
    public Annotation create(Annotation annotation) throws Exception {
        Annotation canonicalObject;
        synchronized (this) {
            canonicalObject = putOrUpdate(facade.create(annotation));
        }
        notifyAnnotationsChanged(getDomainObject(annotation.getTarget()));
        return canonicalObject;
    }
    
    public Annotation save(Annotation annotation) throws Exception {
        Annotation canonicalObject;
        synchronized (this) {
            canonicalObject = putOrUpdate(annotation.getId()==null ? facade.create(annotation) : facade.update(annotation));
        }
        notifyDomainObjectCreated(canonicalObject);
        return canonicalObject;
    }
    
    public void remove(Annotation annotation) throws Exception {
        facade.remove(annotation);
        notifyAnnotationsChanged(getDomainObject(annotation.getTarget()));
    }
    
    public void reorderChildren(TreeNode treeNode, int[] order) throws Exception {
        TreeNode canonicalObject = null;
        synchronized (this) {
            canonicalObject = putOrUpdate(facade.reorderChildren(treeNode, order));
        }
        notifyDomainObjectChanged(canonicalObject);
    }
    
    public TreeNode addChild(TreeNode treeNode, DomainObject domainObject) throws Exception {
        return addChildren(treeNode, Arrays.asList(domainObject));
    }

    public TreeNode addChild(TreeNode treeNode, DomainObject domainObject, Integer index) throws Exception {
        return addChildren(treeNode, Arrays.asList(domainObject), index);
    }
    
    public TreeNode addChildren(TreeNode treeNode, Collection<DomainObject> domainObjects) throws Exception {
        TreeNode canonicalObject = null;
        synchronized (this) {
            canonicalObject = putOrUpdate(facade.addChildren(treeNode, DomainUtils.getReferences(domainObjects), null));
        }
        notifyDomainObjectChanged(canonicalObject);
        return canonicalObject;
    }

    public TreeNode addChildren(TreeNode treeNode, Collection<DomainObject> domainObjects, Integer index) throws Exception {
        TreeNode canonicalObject = null;
        synchronized (this) {
            canonicalObject = putOrUpdate(facade.addChildren(treeNode, DomainUtils.getReferences(domainObjects), index));
        }
        notifyDomainObjectChanged(canonicalObject);
        return canonicalObject;
    }
    
    public TreeNode removeChild(TreeNode treeNode, DomainObject domainObject) throws Exception {
        TreeNode canonicalObject = null;
        synchronized (this) {
            canonicalObject = putOrUpdate(facade.removeChildren(treeNode, DomainUtils.getReferences(Arrays.asList(domainObject))));
        }
        notifyDomainObjectChanged(canonicalObject);
        return canonicalObject;
    }
    
    public TreeNode removeChildren(TreeNode treeNode, Collection<DomainObject> domainObjects) throws Exception {
        TreeNode canonicalObject = null;
        synchronized (this) {
            canonicalObject = putOrUpdate(facade.removeChildren(treeNode, DomainUtils.getReferences(domainObjects)));
        }
        notifyDomainObjectChanged(canonicalObject);
        return canonicalObject;
    }
    
    public TreeNode removeReference(TreeNode treeNode, Reference reference) throws Exception {
        TreeNode canonicalObject = null;
        synchronized (this) {
            canonicalObject = putOrUpdate(facade.removeChildren(treeNode, Arrays.asList(reference)));
        }
        notifyDomainObjectChanged(canonicalObject);
        return canonicalObject;
    }
    
    public ObjectSet addMember(ObjectSet objectSet, DomainObject domainObject) throws Exception {
        ObjectSet canonicalObject = null;
        synchronized (this) {
            canonicalObject = putOrUpdate(facade.addMembers(objectSet, DomainUtils.getReferences(Arrays.asList(domainObject))));
        }
        notifyDomainObjectChanged(canonicalObject);
        return canonicalObject;
    }
    
    public ObjectSet addMembers(ObjectSet objectSet, Collection<DomainObject> domainObjects) throws Exception {
        ObjectSet canonicalObject = null;
        synchronized (this) {
            canonicalObject = putOrUpdate(facade.addMembers(objectSet, DomainUtils.getReferences(domainObjects)));
        }
        notifyDomainObjectChanged(canonicalObject);
        return canonicalObject;
    }
    
    public ObjectSet removeMember(ObjectSet objectSet, DomainObject domainObject) throws Exception {
        ObjectSet canonicalObject = null;
        synchronized (this) {
            canonicalObject = putOrUpdate(facade.removeMembers(objectSet, DomainUtils.getReferences(Arrays.asList(domainObject))));
        }
        notifyDomainObjectChanged(canonicalObject);
        return canonicalObject;
    }
    
    public ObjectSet removeMembers(ObjectSet objectSet, Collection<DomainObject> domainObjects) throws Exception {
        ObjectSet canonicalObject = null;
        synchronized (this) {
            canonicalObject = putOrUpdate(facade.removeMembers(objectSet, DomainUtils.getReferences(domainObjects)));
        }
        notifyDomainObjectChanged(canonicalObject);
        return canonicalObject;
    }
    
    public DomainObject updateProperty(DomainObject domainObject, String propName, String propValue) {
        DomainObject canonicalObject = null;
        synchronized (this) {
            canonicalObject = putOrUpdate(facade.updateProperty(domainObject, propName, propValue));
        }
        notifyDomainObjectChanged(canonicalObject);
        return canonicalObject;
    }

    public DomainObject changePermissions(DomainObject domainObject, String granteeKey, String rights, boolean grant) throws Exception {
        DomainObject canonicalObject = null;
        synchronized (this) {
            canonicalObject = putOrUpdate(facade.changePermissions(domainObject, granteeKey, rights, grant));
        }
        notifyDomainObjectChanged(canonicalObject);
        return canonicalObject;
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
