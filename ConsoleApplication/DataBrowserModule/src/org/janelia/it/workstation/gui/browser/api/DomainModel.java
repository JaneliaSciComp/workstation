package org.janelia.it.workstation.gui.browser.api;

import java.util.*;

import javax.swing.SwingUtilities;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ReverseReference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.domain.ontology.OntologyTermReference;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.LineRelease;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleTile;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.janelia.it.jacs.shared.solr.SolrJsonResults;
import org.janelia.it.jacs.shared.solr.SolrParams;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.DomainFacade;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.OntologyFacade;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.SampleFacade;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.SubjectFacade;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.WorkspaceFacade;
import org.janelia.it.workstation.gui.browser.events.Events;
import org.janelia.it.workstation.gui.browser.events.model.DomainObjectAnnotationChangeEvent;
import org.janelia.it.workstation.gui.browser.events.model.DomainObjectCreateEvent;
import org.janelia.it.workstation.gui.browser.events.model.DomainObjectInvalidationEvent;
import org.janelia.it.workstation.gui.browser.events.model.DomainObjectRemoveEvent;
import org.janelia.it.workstation.gui.browser.model.DomainObjectComparator;
import org.perf4j.LoggingStopWatch;
import org.perf4j.StopWatch;
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
    private static final boolean TIMER = log.isDebugEnabled();

    private final DomainFacade domainFacade;
    private final OntologyFacade ontologyFacade;
    private final SampleFacade sampleFacade;
    private final SubjectFacade subjectFacade;
    private final WorkspaceFacade workspaceFacade;
    
    private final Cache<Reference, DomainObject> objectCache;
    private final Map<Reference, Workspace> workspaceCache;

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
     * Call putOrUpdate(domainObject, false)
     *
     * @param domainObject
     * @return canonical domain object instance
     */
    private <T extends DomainObject> T putOrUpdate(T domainObject) {
        return putOrUpdate(domainObject, false);
    }

    /**
     * Put the object in the cache, or update the cached domain object if there is already a version in the cache.
     * In the latter case, the updated cached instance is returned, and the argument instance can be discarded.
     *
     * If the cache is updated, the object will be invalidated. If invalidateTree is true, then all of its descendants
     * will also be invalidated.
     *
     * @param domainObject
     * @param invalidateTree
     * @param <T>
     * @return
     */
    private <T extends DomainObject> T putOrUpdate(T domainObject, boolean invalidateTree) {
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
                    notifyDomainObjectInvalidated(canonicalObject, invalidateTree);
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
    private List<DomainObject> putOrUpdateAll(Collection<? extends DomainObject> objects) {
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
        notifyDomainObjectsInvalidated(objects);
    }

    private void invalidateInternal(DomainObject domainObject) {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("DomainModel illegally called from EDT!");
        }
        Reference ref = checkIfCanonicalDomainObject(domainObject);
        log.debug("Invalidating cached instance {}", DomainUtils.identify(domainObject));

        objectCache.invalidate(ref);
        workspaceCache.remove(ref);

        // Reload the domain object and stick it into the cache
        DomainObject canonicalDomainObject;
        try {
            canonicalDomainObject = loadDomainObject(ref);
            if (canonicalDomainObject==null) {
                log.warn("Object no longer exists: "+ref);
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
    private DomainObject loadDomainObject(Reference ref) {
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
    public <T extends DomainObject> T getDomainObject(T domainObject) {
        return (T)getDomainObject(Reference.createFor(domainObject));
    }

    /**
     * Retrieve the given domain object, checking the cache first and then the database.
     * @param ref
     * @return canonical domain object instance
     * @throws Exception
     */
    public DomainObject getDomainObject(Reference ref) {
        log.debug("getDomainObject({})",ref);
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
        return getDomainObjectsAs(DomainObject.class, references);
    }

    public <T extends DomainObject> List<T> getDomainObjectsAs(Class<T> domainClass, List<Reference> references) {

        if (references==null) return new ArrayList<>();

        log.debug("getDomainObjects(references.size={})",references.size());
        
        StopWatch w = TIMER ? new LoggingStopWatch() : null;
        
        Map<Reference,T> map = new HashMap<>();
        List<Reference> unsatisfiedRefs = new ArrayList<>();

        for(Reference ref : references) {
            DomainObject domainObject = ref==null?null:objectCache.getIfPresent(ref);
            if (domainObject!=null) {
                map.put(ref, (T)domainObject);
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
                domainObjects.add(putOrUpdate(domainObject));
            }
            else {
                unsatisfiedRefs.add(ref);
            }
        }

        if (TIMER) if (TIMER) w.stop("getDomainObjects(references)");
        log.debug("getDomainObjects: returning {} objects ({} unsatisfied)",domainObjects.size(),unsatisfiedRefs.size());
        return domainObjects;
    }

    public <T extends DomainObject> T getDomainObject(Class<T> domainClass, Long id) {
        List<T> list = getDomainObjects(domainClass, Arrays.asList(id));
        return list.isEmpty() ? null : list.get(0);
    }

    public <T extends DomainObject> List<T> getDomainObjects(Class<T> clazz, List<Long> ids) {
        List<T> objects = new ArrayList<>();
        for(DomainObject domainObject : getDomainObjects(clazz.getSimpleName(), ids)) {
            objects.add((T)domainObject);
        }
        return objects;
    }

    public DomainObject getDomainObject(String className, Long id) {
        List<DomainObject> objects = getDomainObjects(className, Arrays.asList(id));
        return objects.isEmpty() ? null : objects.get(0);
    }

    public <T extends DomainObject> List<T> getDomainObjects(Class<T> domainClass, String name) {
        List<T> objects = new ArrayList<>();
        StopWatch w = TIMER ? new LoggingStopWatch() : null;
        for(DomainObject domainObject : domainFacade.getDomainObjects(domainClass, name)) {
            objects.add((T)domainObject);
        }
        if (TIMER) w.stop("getDomainObjects(Class,objectName)");
        return objects;
    }
    
    public List<DomainObject> getDomainObjects(String className, List<Long> ids) {

        if (className==null || ids==null) return new ArrayList<>();
        log.debug("getDomainObjects(ids.size={})",ids.size());

        StopWatch w = TIMER ? new LoggingStopWatch() : null;
        
        Map<Reference,DomainObject> map = new HashMap<>();
        List<Long> unsatisfiedIds = new ArrayList<>();

        for(Long id : ids) {
            Reference ref = Reference.createFor(className, id);
            DomainObject domainObject = objectCache.getIfPresent(ref);
            if (domainObject!=null) {
                map.put(ref, domainObject);
            }
            else {
                unsatisfiedIds.add(id);
            }
        }

        if (!unsatisfiedIds.isEmpty()) {
            List<DomainObject> objects = domainFacade.getDomainObjects(className, unsatisfiedIds);
            map.putAll(DomainUtils.getMapByReference(objects));
        }

        unsatisfiedIds.clear();

        List<DomainObject> domainObjects = new ArrayList<>();
        for(Long id : ids) {
            Reference ref = Reference.createFor(className, id);
            DomainObject domainObject = map.get(ref);
            if (domainObject!=null) {
                domainObjects.add(putOrUpdate(domainObject));
            }
            else {
                unsatisfiedIds.add(id);
            }
        }

        if (TIMER) w.stop("getDomainObjects(className,ids)");
        log.debug("getDomainObjects: returning {} objects ({} unsatisfied)",domainObjects.size(),unsatisfiedIds.size());
        return domainObjects;
    }
    
    public List<DomainObject> getAllDomainObjectsByClass(String className) {
        List<DomainObject> domainObjects = new ArrayList<>();
        if (className==null) return domainObjects;
        log.debug("getAllDomainObjectsByClass({})",className);

        StopWatch w = TIMER ? new LoggingStopWatch() : null;
        domainObjects = domainFacade.getAllDomainObjectsByClass(className);                
        
        if (TIMER) w.stop("getAllDomainObjectsByClass()");
        log.debug("getAllDomainObjectsByClass: returning {} objects",domainObjects.size());
        return domainObjects;
    }

    public List<DomainObject> getDomainObjects(ReverseReference reverseReference) {
        // TODO: cache these?
        return domainFacade.getDomainObjects(reverseReference);
    }

    public List<Annotation> getAnnotations(Reference reference) {
        // TODO: cache these?
        return ontologyFacade.getAnnotations(Arrays.asList(reference));
    }

    public List<Annotation> getAnnotations(DomainObject domainObject) {
        return getAnnotations(Reference.createFor(domainObject));
    }
    
    public List<Annotation> getAnnotations(Collection<Reference> references) {
        if (references==null) return new ArrayList<>();
        StopWatch w = TIMER ? new LoggingStopWatch() : null;
        List<Annotation> annotations = ontologyFacade.getAnnotations(references); 
        // TODO: cache the annotations?
        if (TIMER) w.stop("getAnnotations(references)");
        return annotations;
    }

    public List<Workspace> getWorkspaces() {
        synchronized (this) {
            if (workspaceCache.isEmpty()) {
                log.debug("Getting workspaces from database");
                StopWatch w = TIMER ? new LoggingStopWatch() : null;
                for (Workspace workspace : workspaceFacade.getWorkspaces()) {
                    Workspace cachedRoot = putOrUpdate(workspace);
                    workspaceCache.put(Reference.createFor(cachedRoot), cachedRoot);
                }
                if (TIMER) w.stop("getWorkspaces");
            }
        }
        List<Workspace> workspaces = new ArrayList<>(workspaceCache.values());
        Collections.sort(workspaces, new DomainObjectComparator());
        return workspaces;

    }

    public Workspace getDefaultWorkspace() {
        return getWorkspaces().iterator().next();
    }

    public List<DataSet> getDataSets() {
        List<DataSet> dataSets = new ArrayList<>();
        StopWatch w = TIMER ? new LoggingStopWatch() : null;
        for (DataSet dataSet : sampleFacade.getDataSets()) {
            dataSets.add(putOrUpdate(dataSet));
        }
        Collections.sort(dataSets, new DomainObjectComparator());
        if (TIMER) w.stop("getDataSets");
        return dataSets;
    }
    
    public List<LSMImage> getLsmsForSample(Sample sample) {
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

    public void remove(List<Reference> deleteObjectRefs) throws Exception {
        List<DomainObject> objects = getDomainObjects(deleteObjectRefs);
        domainFacade.remove(deleteObjectRefs);
        invalidate(objects);
        for(DomainObject object : objects) {
            notifyDomainObjectRemoved(object);
        }
    }

    /**
     * Returns all of the ontologies that a user has access to view. 
     * @return collection of ontologies
     */
    public List<Ontology> getOntologies() {
        List<Ontology> ontologies = new ArrayList<>();
        StopWatch w = TIMER ? new LoggingStopWatch() : null;
        for (Ontology ontology : ontologyFacade.getOntologies()) {
            ontologies.add(putOrUpdate(ontology));
        }
        Collections.sort(ontologies, new DomainObjectComparator());
        if (TIMER) w.stop("getOntologies");
        return ontologies;
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
            canonicalObject = putOrUpdate(ontologyFacade.create(ontology));
        }
        notifyDomainObjectCreated(canonicalObject);
        return canonicalObject;
    }

    public Ontology reorderOntologyTerms(Long ontologyId, Long parentTermId, int[] order) throws Exception {
        Ontology canonicalObject;
        synchronized (this) {
            canonicalObject = putOrUpdate(ontologyFacade.reorderTerms(ontologyId, parentTermId, order));
        }
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
        return canonicalObject;
    }

    public Ontology removeOntologyTerm(Long ontologyId, Long parentTermId, Long termId) throws Exception {
        Ontology canonicalObject;
        synchronized (this) {
            canonicalObject = putOrUpdate(ontologyFacade.removeTerm(ontologyId, parentTermId, termId));
        }
        return canonicalObject;
    }

    public void removeOntology(Long ontologyId) throws Exception {
        Ontology canonicalObject = getDomainObject(Ontology.class, ontologyId);
        synchronized (this) {
            ontologyFacade.removeOntology(ontologyId);
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
        Filter canonicalObject;
        synchronized (this) {
            canonicalObject = putOrUpdate(filter.getId()==null ? workspaceFacade.create(filter) : workspaceFacade.update(filter));
        }
        notifyDomainObjectCreated(canonicalObject);
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
        synchronized (this) {
            canonicalObject = putOrUpdate(dataSet.getId()==null ? sampleFacade.create(dataSet) : sampleFacade.update(dataSet));
        }
        notifyDomainObjectCreated(canonicalObject);
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

    public List<LineRelease> getLineReleases() {
        List<LineRelease> releases = new ArrayList<>();
        StopWatch w = TIMER ? new LoggingStopWatch() : null;
        for (LineRelease release : sampleFacade.getLineReleases()) {
            releases.add(putOrUpdate(release));
        }
        Collections.sort(releases, new DomainObjectComparator());
        if (TIMER) w.stop("getLineReleases");
        return releases;
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
        notifyDomainObjectCreated(canonicalObject);
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
        return canonicalObject;
    }

    public DomainObject updateProperty(DomainObject domainObject, String propName, Object propValue) throws Exception {
        DomainObject canonicalObject;
        synchronized (this) {
            canonicalObject = putOrUpdate(domainFacade.updateProperty(domainObject, propName, propValue));
        }
        return canonicalObject;
    }

    public DomainObject changePermissions(DomainObject domainObject, String granteeKey, String rights, boolean grant) throws Exception {
        DomainObject canonicalObject;
        synchronized (this) {
            canonicalObject = putOrUpdate(domainFacade.changePermissions(domainObject, granteeKey, rights, grant), true);
        }
        return canonicalObject;
    }

    public Subject getSubjectByKey(String subjectKey) throws Exception {
        return subjectFacade.getSubjectByKey(subjectKey);
    }

    public Subject loginSubject(String username, String password) throws Exception {
        return subjectFacade.loginSubject(username, password);
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

    // TODO: This is currently not used because all changes result in invalidated objects. 
    // In the future we may want to keep the same objects and just update them, in which case this event will be useful.
//    private void notifyDomainObjectChanged(DomainObject domainObject) {
//        if (log.isTraceEnabled()) {
//            log.trace("Generating DomainObjectChangeEvent for {}", DomainUtils.identify(domainObject));
//        }
//        Events.getInstance().postOnEventBus(new DomainObjectChangeEvent(domainObject));
//    }

    private void notifyDomainObjectRemoved(DomainObject domainObject) {
        if (log.isTraceEnabled()) {
            log.trace("Generating DomainObjectRemoveEvent for {}", DomainUtils.identify(domainObject));
        }
        Events.getInstance().postOnEventBus(new DomainObjectRemoveEvent(domainObject));
    }

    private void notifyDomainObjectsInvalidated(Collection<? extends DomainObject> objects) {
        if (log.isTraceEnabled()) {
            log.trace("Generating DomainObjectInvalidationEvent with {} entities", objects.size());
        }
        Events.getInstance().postOnEventBus(new DomainObjectInvalidationEvent(objects));
    }

    private void notifyDomainObjectInvalidated(DomainObject domainObject, boolean invalidateTree) {
        if (log.isTraceEnabled()) {
            log.trace("Generating EntityInvalidationEvent for {}", DomainUtils.identify(domainObject));
        }
        Collection<DomainObject> invalidated = new ArrayList<>();
        if (invalidateTree) {
            addTree(invalidated, domainObject);
        }
        else {
            invalidated.add(domainObject);
        }

        Events.getInstance().postOnEventBus(new DomainObjectInvalidationEvent(invalidated));
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
