package org.janelia.it.workstation.browser.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.swing.SwingUtilities;

import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ReverseReference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.domain.ontology.OntologyTermReference;
import org.janelia.it.jacs.model.domain.orders.IntakeOrder;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.LineRelease;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleTile;
import org.janelia.it.jacs.model.domain.sample.StatusTransition;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.support.NotCacheable;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.janelia.it.jacs.shared.solr.SolrJsonResults;
import org.janelia.it.jacs.shared.solr.SolrParams;
import org.janelia.it.workstation.browser.api.facade.interfaces.DomainFacade;
import org.janelia.it.workstation.browser.api.facade.interfaces.OntologyFacade;
import org.janelia.it.workstation.browser.api.facade.interfaces.SampleFacade;
import org.janelia.it.workstation.browser.api.facade.interfaces.SubjectFacade;
import org.janelia.it.workstation.browser.api.facade.interfaces.WorkspaceFacade;
import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.events.model.DomainObjectAnnotationChangeEvent;
import org.janelia.it.workstation.browser.events.model.DomainObjectChangeEvent;
import org.janelia.it.workstation.browser.events.model.DomainObjectCreateEvent;
import org.janelia.it.workstation.browser.events.model.DomainObjectInvalidationEvent;
import org.janelia.it.workstation.browser.events.model.DomainObjectRemoveEvent;
import org.janelia.it.workstation.browser.model.DomainObjectComparator;
import org.perf4j.LoggingStopWatch;
import org.perf4j.StopWatch;
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
    private static final boolean TIMER = log.isDebugEnabled();

    private final DomainFacade domainFacade;
    private final OntologyFacade ontologyFacade;
    private final SampleFacade sampleFacade;
    private final SubjectFacade subjectFacade;
    private final WorkspaceFacade workspaceFacade;

    private final Cache<Reference, DomainObject> objectCache;
    private final Map<Reference, Workspace> workspaceCache;
    private final Map<Reference, Ontology> ontologyCache;

    public DomainModel(DomainFacade domainFacade, OntologyFacade ontologyFacade, SampleFacade sampleFacade, 
            SubjectFacade subjectFacade, WorkspaceFacade workspaceFacade) {
        
        this.domainFacade = domainFacade;
        this.ontologyFacade = ontologyFacade;
        this.sampleFacade = sampleFacade;
        this.subjectFacade = subjectFacade;
        this.workspaceFacade = workspaceFacade;

        this.objectCache = CacheBuilder.newBuilder().softValues().removalListener(new RemovalListener<Reference, DomainObject>() {
            @Override
            public void onRemoval(RemovalNotification<Reference, DomainObject> notification) {
                synchronized (DomainModel.this) {
                    Reference id = notification.getKey();
                    log.trace("removed key from cache: {}",id);
                    workspaceCache.remove(id);
                    ontologyCache.remove(id);
                }
            }
        }).build();
        this.workspaceCache = new LinkedHashMap<>();
        this.ontologyCache = new LinkedHashMap<>();
    }

    /**
     * Check if the given object may be cached at all.
     * @param domainObject
     * @return
     */
    private boolean isCacheable(DomainObject domainObject) {
        return domainObject.getClass().getAnnotation(NotCacheable.class)==null;
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
     * Call putOrUpdate(domainObject, false)
     *
     * @param domainObject
     * @return canonical domain object instance
     */
    public <T extends DomainObject> T putOrUpdate(T domainObject) {
        return putOrUpdate(domainObject, false);
    }

    /**
     * Call putOrUpdate(domainObjects, invalidateTree)
     *
     * @param domainObject
     * @return canonical domain object instance
     */
    private <T extends DomainObject> T putOrUpdate(T domainObject, boolean invalidateTree) {
        List<T> canonicalObjects = putOrUpdate(Arrays.asList(domainObject), invalidateTree);
        if (canonicalObjects.isEmpty()) return null;
        return canonicalObjects.get(0);
    }

    /**
     * Put the object in the cache, or update the cached domain object if there is already a version in the cache.
     * In the latter case, the updated cached instance is returned, and the argument instance can be discarded.
     *
     * If the cache is updated, the object will be invalidated. If invalidateTree is true, then all of its descendants
     * will also be invalidated.
     *
     * @param domainObjects
     * @param invalidateTree
     * @param <T>
     * @return
     */
    public <T extends DomainObject> List<T> putOrUpdate(Collection<T> domainObjects, boolean invalidateTree) {
        List<T> canonicalObjects = new ArrayList<>();
        if (domainObjects == null || domainObjects.isEmpty()) {
            // This is a null object, which cannot go into the cache
            log.debug("putOrUpdate: object is null");
            return canonicalObjects;
        }
        List<T> invalidatedObjects = new ArrayList<>();
        synchronized (this) {
            for(T domainObject : domainObjects) {
                if (domainObject==null) continue;
                if (!isCacheable(domainObject)) {
                    log.warn("Cannot cache domain object annotated with @NotCacheable: {}", domainObject);
                    continue;
                }
                Reference id = Reference.createFor(domainObject);
                @SuppressWarnings("unchecked")
                T canonicalObject = (T)objectCache.getIfPresent(id);
                if (canonicalObject != null) {
                    if (canonicalObject != domainObject) {
                        canonicalObject = domainObject;
                        log.debug("putOrUpdate: Updating cached instance: {} {}", id, DomainUtils.identify(canonicalObject));
                        updateCaches(id, domainObject);
                        invalidatedObjects.add(domainObject);
                    }
                    else {
                        log.debug("putOrUpdate: Returning cached instance: {} {}", id, DomainUtils.identify(canonicalObject));
                    }
                }
                else {
                    canonicalObject = domainObject;
                    log.debug("putOrUpdate: Caching: {} {}", id, DomainUtils.identify(canonicalObject));
                    updateCaches(id, domainObject);
                }

                canonicalObjects.add(canonicalObject);
            }

            if (!invalidatedObjects.isEmpty()) {
                notifyDomainObjectsInvalidated(invalidatedObjects, invalidateTree);
            }
        }
        return canonicalObjects;
    }

    private void updateCaches(Reference id, DomainObject domainObject) {
        objectCache.put(id, domainObject);
        if (domainObject instanceof Workspace) {
            workspaceCache.put(id, (Workspace)domainObject);
        }
        else if (domainObject instanceof Ontology) {
            ontologyCache.put(id, (Ontology)domainObject);
        }
    }

    /**
     * Clear the entire cache without raising any events. This is basically only useful for changing logins.
     */
    public void invalidateAll() {
        log.debug("Invalidating all objects");
        objectCache.invalidateAll();
        workspaceCache.clear();
        ontologyCache.clear();
        Events.getInstance().postOnEventBus(new DomainObjectInvalidationEvent());
    }

    /**
     * Invalidate the domain object in the cache, so that it will be reloaded on the next request.
     *
     * @param domainObject
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
     */
    public void invalidate(Collection<? extends DomainObject> objects) {
        log.debug("Invalidating {} objects", objects.size());
        for (DomainObject domainObject : objects) {
            invalidateInternal(domainObject);
        }
        notifyDomainObjectsInvalidated(objects, false);
    }

    private void invalidateInternal(DomainObject domainObject) {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("DomainModel illegally called from EDT!");
        }
        Reference ref = checkIfCanonicalDomainObject(domainObject);
        log.debug("Invalidating cached instance {}", DomainUtils.identify(domainObject));

        objectCache.invalidate(ref);
        workspaceCache.remove(ref);
        ontologyCache.remove(ref);

        // Reload the domain object and stick it into the cache
        DomainObject canonicalDomainObject;
        try {
            canonicalDomainObject = loadDomainObject(ref);
            if (canonicalDomainObject==null) {
                log.trace("Cannot reload object which no longer exists: "+ref);
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
     * @param ref
     * @return
     */
    private <T extends DomainObject> T loadDomainObject(Reference ref) throws Exception {
        log.debug("loadDomainObject({})", ref);
        return domainFacade.getDomainObject(ref);
    }

    /**
     * Reload the given domain object from the database, and update the cache.
     *
     * @param domainObject
     * @return canonical domain object instance
     * @throws Exception
     */
    public <T extends DomainObject> T reload(T domainObject) throws Exception {
        return reloadById(Reference.createFor(domainObject));
    }

    /**
     * Reload the given domain object from the database, and update the cache.
     *
     * @param Reference
     * @return canonical domain object instance
     * @throws Exception
     */
    public <T extends DomainObject> T reloadById(Reference Reference) throws Exception {
        synchronized (this) {
            T domainobject = loadDomainObject(Reference);
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
            return putOrUpdate(objs, false);
        }
    }

    /**
     * Retrieve the given domain object, checking the cache first and then the database.
     * @param domainObject
     * @return canonical domain object instance
     */
    public <T extends DomainObject> T getDomainObject(T domainObject) throws Exception {
        if (domainObject==null) throw new IllegalArgumentException("Domain object may not be null");
        if (!isCacheable(domainObject)) {
            return domainObject;
        }
        return getDomainObject(Reference.createFor(domainObject));
    }

    /**
     * Retrieve the given domain object, checking the cache first and then the database.
     * @param ref
     * @return canonical domain object instance
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public <T extends DomainObject> T getDomainObject(Reference ref) throws Exception {
        log.debug("getDomainObject({})",ref);
        if (ref==null) return null;
        DomainObject domainObject = objectCache.getIfPresent(ref);
        if (domainObject != null) {
            log.debug("getEntityById: returning cached domain object {}", DomainUtils.identify(domainObject));
            return (T)domainObject;
        }
        synchronized (this) {
            return putOrUpdate((T)loadDomainObject(ref));
        }
    }

    public List<DomainObject> getDomainObjects(List<Reference> references) throws Exception {
        return getDomainObjectsAs(DomainObject.class, references);
    }

    @SuppressWarnings("unchecked")
    public <T extends DomainObject> List<T> getDomainObjectsAs(Class<T> domainClass, List<Reference> references) throws Exception {

        if (references==null) return new ArrayList<>();

        log.debug("getDomainObjects(references.size={})",references.size());
        
        StopWatch w = TIMER ? new LoggingStopWatch() : null;
        
        Map<Reference,T> map = new HashMap<>();
        List<Reference> unsatisfiedRefs = new ArrayList<>();

        for(Reference ref : references) {
            T domainObject = ref==null?null:(T)objectCache.getIfPresent(ref);
            if (domainObject!=null) {
                map.put(ref, domainObject);
            }
            else {
                unsatisfiedRefs.add(ref);
            }
        }
                
        if (!unsatisfiedRefs.isEmpty()) {
            List<DomainObject> objects = domainFacade.getDomainObjects(unsatisfiedRefs);
            List<T> classObjects = new ArrayList<>();
            for(DomainObject domainObject : objects) {
                classObjects.add((T)domainObject);
            }
            map.putAll(DomainUtils.getMapByReference(classObjects));
        }

        unsatisfiedRefs.clear();

        List<T> domainObjects = new ArrayList<>();
        for(Reference ref : references) {
            T domainObject = map.get(ref);
            if (domainObject!=null) {
                domainObjects.add(domainObject);
            }
            else {
                unsatisfiedRefs.add(ref);
            }
        }

        List<T> canonicalObjects = putOrUpdate(domainObjects, false);
        if (TIMER) if (TIMER) w.stop("getDomainObjects(references)");
        log.debug("getDomainObjects: returning {} objects ({} unsatisfied)",canonicalObjects.size(),unsatisfiedRefs.size());
        return canonicalObjects;
    }

    public <T extends DomainObject> T getDomainObject(Class<T> domainClass, Long id) throws Exception {
        List<T> list = getDomainObjects(domainClass, Arrays.asList(id));
        return list.isEmpty() ? null : list.get(0);
    }

    @SuppressWarnings("unchecked")
    public <T extends DomainObject> List<T> getDomainObjects(Class<T> clazz, List<Long> ids) throws Exception {
        List<T> objects = new ArrayList<>();
        for(DomainObject domainObject : getDomainObjects(clazz.getSimpleName(), ids)) {
            objects.add((T)domainObject);
        }
        return objects;
    }

    public DomainObject getDomainObject(String className, Long id) throws Exception {
        List<DomainObject> objects = getDomainObjects(className, Arrays.asList(id));
        return objects.isEmpty() ? null : objects.get(0);
    }

    public <T extends DomainObject> List<T> getDomainObjects(Class<T> domainClass, String name) throws Exception {
        List<T> objects = new ArrayList<>();
        StopWatch w = TIMER ? new LoggingStopWatch() : null;
        for(T domainObject : domainFacade.getDomainObjects(domainClass, name)) {
            objects.add(domainObject);
        }
        if (TIMER) w.stop("getDomainObjects(Class,objectName)");
        return objects;
    }
    
    public <T extends DomainObject> List<T> getDomainObjects(String className, List<Long> ids) throws Exception {

        if (className==null || ids==null) return new ArrayList<>();
        log.debug("getDomainObjects(ids.size={})",ids.size());

        StopWatch w = TIMER ? new LoggingStopWatch() : null;
        
        Map<Reference,T> map = new HashMap<>();
        List<Long> unsatisfiedIds = new ArrayList<>();

        for(Long id : ids) {
            Reference ref = Reference.createFor(className, id);
            @SuppressWarnings("unchecked")
            T domainObject = (T)objectCache.getIfPresent(ref);
            if (domainObject!=null) {
                map.put(ref, domainObject);
            }
            else {
                unsatisfiedIds.add(id);
            }
        }

        if (!unsatisfiedIds.isEmpty()) {
            List<T> objects = domainFacade.getDomainObjects(className, unsatisfiedIds);
            map.putAll(DomainUtils.getMapByReference(objects));
        }

        unsatisfiedIds.clear();

        List<T> domainObjects = new ArrayList<>();
        for(Long id : ids) {
            Reference ref = Reference.createFor(className, id);
            T domainObject = map.get(ref);
            if (domainObject!=null) {
                domainObjects.add(domainObject);
            }
            else {
                unsatisfiedIds.add(id);
            }
        }

        List<T> canonicalObjects = putOrUpdate(domainObjects, false);
        if (TIMER) w.stop("getDomainObjects(className,ids)");
        log.debug("getDomainObjects: returning {} objects ({} unsatisfied)",canonicalObjects.size(),unsatisfiedIds.size());
        return canonicalObjects;
    }
    
    public List<DomainObject> getAllDomainObjectsByClass(String className) throws Exception {
        List<DomainObject> domainObjects = new ArrayList<>();
        if (className==null) return domainObjects;
        log.debug("getAllDomainObjectsByClass({})",className);

        StopWatch w = TIMER ? new LoggingStopWatch() : null;
        domainObjects = domainFacade.getAllDomainObjectsByClass(className);                
        
        if (TIMER) w.stop("getAllDomainObjectsByClass()");
        log.debug("getAllDomainObjectsByClass: returning {} objects",domainObjects.size());
        return domainObjects;
    }

    public List<DomainObject> getDomainObjects(ReverseReference reverseReference) throws Exception {
        // TODO: cache these?
        return domainFacade.getDomainObjects(reverseReference);
    }

    public List<Annotation> getAnnotations(DomainObject domainObject) throws Exception {
        return getAnnotations(Reference.createFor(domainObject));
    }
    
    public List<Annotation> getAnnotations(Reference reference) throws Exception {
        return getAnnotations(Arrays.asList(reference));
    }

    public List<Annotation> getAnnotations(Collection<Reference> references) throws Exception {
        if (references==null) return new ArrayList<>();
        StopWatch w = TIMER ? new LoggingStopWatch() : null;
        List<Annotation> annotations = ontologyFacade.getAnnotations(references); 
        // TODO: cache the annotations?
        if (TIMER) w.stop("getAnnotations(references)");
        return annotations;
    }

    public List<Workspace> getWorkspaces() throws Exception {
        synchronized (this) {
            if (workspaceCache.isEmpty()) {
                log.debug("Getting workspaces from database");
                StopWatch w = TIMER ? new LoggingStopWatch() : null;
                Collection<Workspace> ontologies = workspaceFacade.getWorkspaces();
                List<Workspace> canonicalObjects = putOrUpdate(ontologies, false);
                Collections.sort(canonicalObjects, new DomainObjectComparator());
                for (Workspace workspace : canonicalObjects) {
                    workspaceCache.put(Reference.createFor(workspace), workspace);
                }
                if (TIMER) w.stop("getWorkspaces");
            }
        }
        return new ArrayList<>(workspaceCache.values());
    }

    public Workspace getDefaultWorkspace() throws Exception {
        for (Workspace workspace : getWorkspaces()) {
            if (workspace.getOwnerKey().equals(AccessManager.getSubjectKey()) && DomainConstants.NAME_DEFAULT_WORKSPACE.equals(workspace.getName())) {
                return workspace;
            }
        }
        throw new IllegalStateException("Cannot find default workspace");
    }
    
    public TreeNode getDefaultWorkspaceFolder(String folderName, boolean createIfNecessary) throws Exception {
        Workspace workspace = getDefaultWorkspace();
        List<DomainObject> children = getDomainObjects(workspace.getChildren());
        TreeNode treeNode = DomainUtils.findObjectByTypeAndName(children, TreeNode.class, folderName);
        if (treeNode==null && createIfNecessary) {
            treeNode = new TreeNode();
            treeNode.setName(folderName);
            treeNode = create(treeNode);
            addChild(workspace, treeNode);
        }
        return treeNode;
    }

    public List<DataSet> getDataSets() throws Exception {
        StopWatch w = TIMER ? new LoggingStopWatch() : null;
        List<DataSet> dataSets = new ArrayList<>(sampleFacade.getDataSets());
        List<DataSet> canonicalDataSets = putOrUpdate(dataSets, false);
        Collections.sort(canonicalDataSets, new DomainObjectComparator());
        if (TIMER) w.stop("getDataSets");
        return canonicalDataSets;
    }
    
    public List<LSMImage> getLsmsForSample(Sample sample) throws Exception {
        StopWatch w = TIMER ? new LoggingStopWatch() : null;
        List<Reference> lsmRefs = new ArrayList<>();
        for(ObjectiveSample objectiveSample : sample.getObjectiveSamples()) {
            for(SampleTile tile : objectiveSample.getTiles()) {
                lsmRefs.addAll(tile.getLsmReferences());
            }
        }
        List<LSMImage> images = getDomainObjectsAs(LSMImage.class, lsmRefs);
        if (TIMER) w.stop("getLsmsForSample");
        return images;
    }

    public List<Reference> getContainerReferences(DomainObject object) {
        try {
            StopWatch w = TIMER ? new LoggingStopWatch() : null;
            List<Reference> references = workspaceFacade.getContainerReferences(object);
            if (TIMER) w.stop("getContainerReferences");
            return references;
        }
        catch (Exception e) {
            log.error("Problems checking references to object " + object.getId(),e);
        }
        return null;
    }
    
    public void remove(List<? extends DomainObject> domainObjects) throws Exception {
        domainFacade.remove(DomainUtils.getReferences(domainObjects));
        invalidate(domainObjects);
        for(DomainObject object : domainObjects) {
            notifyDomainObjectRemoved(object);
        }
    }

    /**
     * Returns all of the ontologies that a user has access to view. 
     * @return collection of ontologies
     */
    public List<Ontology> getOntologies() {
        synchronized (this) {
            if (ontologyCache.isEmpty()) {
                log.debug("Getting ontologies from database");
                StopWatch w = TIMER ? new LoggingStopWatch() : null;
                Collection<Ontology> ontologies = ontologyFacade.getOntologies();
                List<Ontology> canonicalObjects = putOrUpdate(ontologies, false);
                Collections.sort(canonicalObjects, new DomainObjectComparator());
                for (Ontology ontology : canonicalObjects) {
                    ontologyCache.put(Reference.createFor(ontology), ontology);
                }
                if (TIMER) w.stop("getOntologies");
            }
        }
        return new ArrayList<>(ontologyCache.values());
    }

    /**
     * Returns an ordered set of all the unique terms in an ontology tree, rooted at the given term.  
     * @param ontologyTerm root of an ontology tree or sub-tree. 
     * @return ordered set of ontology term names
     */
    public TreeSet<String> getOntologyTermSet(OntologyTerm ontologyTerm) {
        TreeSet<String> terms = new TreeSet<>();
        if (ontologyTerm.hasChildren()) {
            for (OntologyTerm childTerm : ontologyTerm.getTerms()) {
                terms.add(childTerm.getName());
                terms.addAll(getOntologyTermSet(childTerm));
            }
        }
        return terms;
    }

    /**
     * Returns the ontology term specified by the given reference, by looking up the relevant ontology 
     * and recursively walking it to find the term. 
     * @param reference a reference to an ontology term
     * @return the ontology term 
     */
    public OntologyTerm getOntologyTermByReference(OntologyTermReference reference) throws Exception {
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
            canonicalObject = putOrUpdate(ontologyFacade.create(ontology));
            ontologyCache.put(Reference.createFor(canonicalObject), canonicalObject);
        }
        notifyDomainObjectCreated(canonicalObject);
        return canonicalObject;
    }

    public Ontology reorderOntologyTerms(Long ontologyId, Long parentTermId, int[] order) throws Exception {
        Ontology canonicalObject;
        synchronized (this) {
            canonicalObject = putOrUpdate(ontologyFacade.reorderTerms(ontologyId, parentTermId, order));
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
            canonicalObject = putOrUpdate(ontologyFacade.addTerms(ontologyId, parentTermId, terms, index));
        }
        notifyDomainObjectChanged(canonicalObject);
        return canonicalObject;
    }

    public Ontology removeOntologyTerm(Long ontologyId, Long parentTermId, Long termId) throws Exception {
        Ontology canonicalObject;
        synchronized (this) {
            canonicalObject = putOrUpdate(ontologyFacade.removeTerm(ontologyId, parentTermId, termId));
        }
        notifyDomainObjectChanged(canonicalObject);
        return canonicalObject;
    }

    public void removeOntology(Long ontologyId) throws Exception {
        Ontology canonicalObject = getDomainObject(Ontology.class, ontologyId);
        synchronized (this) {
            ontologyFacade.removeOntology(ontologyId);
            ontologyCache.remove(Reference.createFor(canonicalObject));
        }
        notifyDomainObjectRemoved(canonicalObject);
    }

    public TreeNode create(TreeNode treeNode) throws Exception {
        TreeNode canonicalObject;
        synchronized (this) {
            canonicalObject = putOrUpdate(workspaceFacade.create(treeNode));
        }
        notifyDomainObjectCreated(canonicalObject);
        return canonicalObject;
    }

    public Filter save(Filter filter) throws Exception {
        boolean create = filter.getId()==null;
        Filter canonicalObject;
        synchronized (this) {
            canonicalObject = putOrUpdate(create ? workspaceFacade.create(filter) : workspaceFacade.update(filter));
        }
        if (create) { 
            notifyDomainObjectCreated(canonicalObject);
        }
        else {
            notifyDomainObjectChanged(canonicalObject);
        }
        return canonicalObject;
    }

    public SolrJsonResults search(SolrParams query) throws Exception {
        StopWatch w = TIMER ? new LoggingStopWatch() : null;
        SolrJsonResults results = workspaceFacade.performSearch(query);
        if (TIMER) w.stop("search(query)");
        return results;
    }

    public DataSet save(DataSet dataSet) throws Exception {
        DataSet canonicalObject;
        boolean create = dataSet.getId()==null;
        synchronized (this) {
            canonicalObject = putOrUpdate(create ? sampleFacade.create(dataSet) : sampleFacade.update(dataSet));
        }
        if (create) { 
            notifyDomainObjectCreated(canonicalObject);
        }
        else {
            notifyDomainObjectChanged(canonicalObject);
        }

        TreeNode folder = getDefaultWorkspaceFolder(DomainConstants.NAME_DATA_SETS, false);
        if (folder != null) {
            invalidate(folder);
        }
        
        return canonicalObject;
    }

    public void remove(DataSet dataSet) throws Exception {
        sampleFacade.remove(dataSet);
        notifyDomainObjectRemoved(dataSet);
    }

    public Annotation createAnnotation(Reference target, OntologyTermReference ontologyTermReference, Object value) throws Exception {
        Annotation canonicalObject;
        synchronized (this) {
            canonicalObject = putOrUpdate(ontologyFacade.createAnnotation(target, ontologyTermReference, value));
        }
        notifyAnnotationsChanged(getDomainObject(canonicalObject.getTarget()));
        return canonicalObject;
    }
    
    public Annotation create(Annotation annotation) throws Exception {
        Annotation canonicalObject;
        synchronized (this) {
            canonicalObject = putOrUpdate(ontologyFacade.create(annotation));
        }
        notifyAnnotationsChanged(getDomainObject(annotation.getTarget()));
        return canonicalObject;
    }

    public Annotation save(Annotation annotation) throws Exception {
        Annotation canonicalObject;
        synchronized (this) {
            canonicalObject = putOrUpdate(annotation.getId()==null ? ontologyFacade.create(annotation) : ontologyFacade.update(annotation));
        }
        notifyAnnotationsChanged(getDomainObject(annotation.getTarget()));
        return canonicalObject;
    }

    public void remove(Annotation annotation) throws Exception {
        ontologyFacade.remove(annotation);
        notifyAnnotationsChanged(getDomainObject(annotation.getTarget()));
    }

    public TreeNode reorderChildren(TreeNode treeNode, int[] order) throws Exception {
        TreeNode canonicalObject = null;
        synchronized (this) {
            canonicalObject = putOrUpdate(workspaceFacade.reorderChildren(treeNode, order));
        }
        return canonicalObject;
    }

    public TreeNode addChild(TreeNode treeNode, DomainObject domainObject) throws Exception {
        return addChildren(treeNode, Arrays.asList(domainObject));
    }

    public TreeNode addChild(TreeNode treeNode, DomainObject domainObject, Integer index) throws Exception {
        return addChildren(treeNode, Arrays.asList(domainObject), index);
    }

    public TreeNode addChildren(TreeNode treeNode, Collection<? extends DomainObject> domainObjects) throws Exception {
        return addChildren(treeNode, domainObjects, null);
    }

    public TreeNode addChildren(TreeNode treeNode, Collection<? extends DomainObject> domainObjects, Integer index) throws Exception {
        TreeNode canonicalObject = null;
        synchronized (this) {
            canonicalObject = putOrUpdate(workspaceFacade.addChildren(treeNode, DomainUtils.getReferences(domainObjects), index));
        }
        return canonicalObject;
    }

    public TreeNode removeChild(TreeNode treeNode, DomainObject domainObject) throws Exception {
        return removeChildren(treeNode, Arrays.asList(domainObject));
    }

    public TreeNode removeChildren(TreeNode treeNode, Collection<? extends DomainObject> domainObjects) throws Exception {
        TreeNode canonicalObject = null;
        synchronized (this) {
            canonicalObject = putOrUpdate(workspaceFacade.removeChildren(treeNode, DomainUtils.getReferences(domainObjects)));
        }
        return canonicalObject;
    }

    public TreeNode removeReference(TreeNode treeNode, Reference reference) throws Exception {
        TreeNode canonicalObject = null;
        synchronized (this) {
            canonicalObject = putOrUpdate(workspaceFacade.removeChildren(treeNode, Arrays.asList(reference)));
        }
        return canonicalObject;
    }

    public List<LineRelease> getLineReleases() throws Exception {
        StopWatch w = TIMER ? new LoggingStopWatch() : null;
        List<LineRelease> releases = sampleFacade.getLineReleases();
        List<LineRelease> canonicalReleases = putOrUpdate(releases, false);
        Collections.sort(canonicalReleases, new DomainObjectComparator());
        if (TIMER) w.stop("getLineReleases");
        return canonicalReleases;
    }

    public LineRelease createLineRelease(String name, Date releaseDate, Integer lagTimeFinal, List<String> dataSets) throws Exception {
        LineRelease canonicalObject;
        synchronized (this) {
            canonicalObject = putOrUpdate(sampleFacade.createLineRelease(name, releaseDate, lagTimeFinal, dataSets));
        }
        notifyDomainObjectCreated(canonicalObject);
        return canonicalObject;
    }
    
    public LineRelease update(LineRelease release) throws Exception {
        LineRelease canonicalObject;
        synchronized (this) {
            canonicalObject = putOrUpdate(sampleFacade.update(release));
        }
        notifyDomainObjectChanged(canonicalObject);
        return canonicalObject;
    }

    public void remove(LineRelease release) throws Exception {
        sampleFacade.remove(release);
        notifyDomainObjectRemoved(release);
    }

    public DomainObject save(DomainObject domainObject) throws Exception {
        DomainObject canonicalObject;
        synchronized (this) {
            canonicalObject = putOrUpdate(domainFacade.save(domainObject));
        }
        notifyDomainObjectChanged(canonicalObject);
        return canonicalObject;
    }

    public DomainObject updateProperty(DomainObject domainObject, String propName, Object propValue) throws Exception {
        DomainObject canonicalObject;
        synchronized (this) {
            canonicalObject = putOrUpdate(domainFacade.updateProperty(domainObject, propName, propValue));
        }
        notifyDomainObjectChanged(canonicalObject);
        return canonicalObject;
    }

    public DomainObject changePermissions(DomainObject domainObject, String granteeKey, String rights) throws Exception {
        DomainObject canonicalObject;
        synchronized (this) {
            canonicalObject = putOrUpdate(domainFacade.setPermissions(domainObject, granteeKey, rights), true);
        }
        notifyDomainObjectChanged(canonicalObject);
        return canonicalObject;
    }

    public Subject getSubjectByKey(String subjectKey) throws Exception {
        return subjectFacade.getSubjectByKey(subjectKey);
    }

    public Subject loginSubject(String username, String password) throws Exception {
        return subjectFacade.loginSubject(username, password);
    }

    public void notifyDomainObjectCreated(DomainObject domainObject) {
        if (log.isTraceEnabled()) {
            log.trace("Generating DomainObjectCreateEvent for {}", DomainUtils.identify(domainObject));
        }
        Events.getInstance().postOnEventBus(new DomainObjectCreateEvent(domainObject));
    }

    public void notifyDomainObjectChanged(DomainObject domainObject) {
        if (log.isTraceEnabled()) {
            log.trace("Generating DomainObjectChangeEvent for {}", DomainUtils.identify(domainObject));
        }
        Events.getInstance().postOnEventBus(new DomainObjectChangeEvent(domainObject));
    }

    public void notifyAnnotationsChanged(DomainObject domainObject) {
        if (log.isTraceEnabled()) {
            log.trace("Generating DomainObjectAnnotationChangeEvent for {}", DomainUtils.identify(domainObject));
        }
        Events.getInstance().postOnEventBus(new DomainObjectAnnotationChangeEvent(domainObject));
    }

    public void notifyDomainObjectRemoved(DomainObject domainObject) {
        if (log.isTraceEnabled()) {
            log.trace("Generating DomainObjectRemoveEvent for {}", DomainUtils.identify(domainObject));
        }
        Events.getInstance().postOnEventBus(new DomainObjectRemoveEvent(domainObject));
    }

    public void addPipelineStatusTransition(StatusTransition transition) throws Exception {
        sampleFacade.addStatusTransition(transition);
    }

    public void addIntakeOrder (IntakeOrder order) throws Exception {
        sampleFacade.addIntakeOrder(order);
    }

    private void notifyDomainObjectsInvalidated(Collection<? extends DomainObject> objects, boolean invalidateTree) {
        if (log.isTraceEnabled()) {
            log.trace("Generating DomainObjectInvalidationEvent with {} entities", objects.size());
        }

        Collection<DomainObject> invalidated = new ArrayList<>();
        if (invalidateTree) {
            for(DomainObject domainObject : objects) {
                addTree(invalidated, domainObject);
            }
        }
        else {
            invalidated.addAll(objects);
        }

        if (!invalidated.isEmpty()) {
            Events.getInstance().postOnEventBus(new DomainObjectInvalidationEvent(invalidated));
        }
    }

    private void addTree(Collection<DomainObject> objects, DomainObject domainObject) {

        if (domainObject instanceof TreeNode) {
            TreeNode treeNode = (TreeNode)domainObject;
            for(Reference childRef : treeNode.getChildren()) {
                DomainObject childObj = objectCache.getIfPresent(childRef);
                if (childObj!=null) {
                    objectCache.invalidate(childRef);
                    addTree(objects, childObj);
                }
            }
        }
        else if (domainObject instanceof Sample) {
            // TODO: Should handle invalidation of LSMs and neuron fragments
        }

        objects.add(domainObject);
    }
}
