package org.janelia.workstation.core.api;

import com.google.common.cache.*;
import com.google.common.collect.ComparisonChain;
import org.janelia.it.jacs.model.entity.json.JsonTask;
import org.janelia.model.access.domain.search.DocumentSearchParams;
import org.janelia.model.access.domain.search.DocumentSearchResults;
import org.janelia.model.domain.*;
import org.janelia.model.domain.compute.ContainerizedService;
import org.janelia.model.domain.dto.SampleReprocessingRequest;
import org.janelia.model.domain.flyem.EMDataSet;
import org.janelia.model.domain.gui.cdmip.ColorDepthLibrary;
import org.janelia.model.domain.gui.cdmip.ColorDepthMask;
import org.janelia.model.domain.gui.cdmip.ColorDepthSearch;
import org.janelia.model.domain.gui.search.Filter;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.ontology.Ontology;
import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.model.domain.ontology.OntologyTermReference;
import org.janelia.model.domain.sample.*;
import org.janelia.model.domain.support.NotCacheable;
import org.janelia.model.domain.workspace.Node;
import org.janelia.model.domain.workspace.TreeNode;
import org.janelia.model.domain.workspace.Workspace;
import org.janelia.model.security.Subject;
import org.janelia.workstation.core.api.facade.interfaces.*;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.model.*;
import org.janelia.workstation.core.model.search.DomainObjectResultPage;
import org.janelia.workstation.core.model.search.DomainObjectSearchResults;
import org.janelia.workstation.core.util.ColorDepthUtils;
import org.perf4j.LoggingStopWatch;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implements a unified domain model for client-side operations. All changes to the model should go through this class, as well as any domain object accesses
 * which need to maintain the domain object in memory for more than one user interaction.
 * <p>
 * A few guiding principles:
 * <p>
 * 1) The methods in this class can potentially access the data source, so they should ALWAYS be called from a worker thread, never from the EDT. By
 * contrast, all resulting events are queued on the EDT, so that GUI widgets can immediately make use of the results.
 * <p>
 * 2) Only one instance of any given domain object is ever maintained by this model. If the domain object has changed, the existing instance is updated with new
 * information. Duplicate instances are always discarded. The canonical instance (i.e. cached instance) is returned by most methods, and clients can
 * synchronize with the cache by using those instances, or by responding to the associated invalidation events.
 * <p>
 * 3) Invalidating a given domain object removes it from the cache entirely. Clients are encouraged to listen for the corresponding DomainObjectInvalidationEvent,
 * and discard their references to invalidated entities.
 * <p>
 * 4) This class is intended to be thread-safe. Updates to the cache are atomic (synchronized to the DomainObjectModel instance).
 * <p>
 * 5) Cached entities may expire at any time, if they are not referenced outside of the cache. Therefore, you should never write any code that relies
 * on entities being cached or uncached.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainModel {

    private static final Logger log = LoggerFactory.getLogger(DomainModel.class);
    private static final boolean TIMER = log.isDebugEnabled();

    private final Object modelLock = new Object();

    private final DomainFacade domainFacade;
    private final OntologyFacade ontologyFacade;
    private final SampleFacade sampleFacade;
    private final SubjectFacade subjectFacade;
    private final WorkspaceFacade workspaceFacade;

    private final Cache<Reference, DomainObject> objectCache;
    private Map<Reference, Workspace> workspaceCache;
    private Map<Reference, Ontology> ontologyCache;
    private Map<Reference, LineRelease> releaseCache;
    private Map<Reference, ContainerizedService> containerCache;

    private final LoadingCache<DocumentSearchParams, DocumentSearchResults> cachedSearchResults = CacheBuilder.newBuilder()
            .maximumSize(20)
            .build(
                    new CacheLoader<DocumentSearchParams, DocumentSearchResults>() {
                        @Override
                        public DocumentSearchResults load(DocumentSearchParams query) throws Exception {
                            return workspaceFacade.performSearch(query);
                        }
                    });

    public DomainModel(DomainFacade domainFacade, OntologyFacade ontologyFacade, SampleFacade sampleFacade,
                       SubjectFacade subjectFacade, WorkspaceFacade workspaceFacade) {

        this.domainFacade = domainFacade;
        this.ontologyFacade = ontologyFacade;
        this.sampleFacade = sampleFacade;
        this.subjectFacade = subjectFacade;
        this.workspaceFacade = workspaceFacade;

        this.objectCache = CacheBuilder.newBuilder().softValues().removalListener((RemovalListener<Reference, DomainObject>) notification -> {
            synchronized (modelLock) {
                Reference id = notification.getKey();
                log.trace("Removed key from caches: {}", id);
                if (workspaceCache != null) workspaceCache.remove(id);
                if (ontologyCache != null) ontologyCache.remove(id);
                if (releaseCache != null) releaseCache.remove(id);
                if (containerCache != null) containerCache.remove(id);
            }
        }).build();
    }

    /**
     * Check if the given object may be cached at all.
     *
     * @param domainObject
     * @return
     */
    private boolean isCacheable(DomainObject domainObject) {
        return domainObject.getClass().getAnnotation(NotCacheable.class) == null;
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
            } else if (presentObject != domainObject) {
                throw new IllegalStateException("DomainModel: Instance mismatch: " + domainObject.getName()
                        + " (cached=" + System.identityHashCode(presentObject)
                        + ") vs (this=" + System.identityHashCode(domainObject) + ")");
            }
        } catch (IllegalStateException e) {
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
        List<T> canonicalObjects = putOrUpdate(Collections.singletonList(domainObject), invalidateTree);
        if (canonicalObjects.isEmpty()) return null;
        return canonicalObjects.get(0);
    }

    /**
     * Call putOrUpdate(domainObjects, false)
     *
     * @param domainObjects
     * @return canonical domain object instance
     */
    public <T extends DomainObject> List<T> putOrUpdate(Collection<T> domainObjects) {
        return putOrUpdate(domainObjects, false);
    }

    /**
     * Put the object in the cache, or update the cached domain object if there is already a version in the cache.
     * In the latter case, the updated cached instance is returned, and the argument instance can be discarded.
     * <p>
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
            log.debug("putOrUpdate(): object list is empty");
            return canonicalObjects;
        }
        List<T> invalidatedObjects = new ArrayList<>();
        synchronized (modelLock) {
            for (T domainObject : domainObjects) {
                if (domainObject == null) continue;
                if (!isCacheable(domainObject)) {
                    log.warn("Cannot cache domain object annotated with @NotCacheable: {}", domainObject);
                    continue;
                }
                Reference id = Reference.createFor(domainObject);
                @SuppressWarnings("unchecked")
                T canonicalObject = (T) objectCache.getIfPresent(id);
                if (canonicalObject != null) {
                    if (canonicalObject != domainObject) {
                        log.debug("putOrUpdate({}): Updating cached instance {} with {}", id, DomainUtils.identify(canonicalObject), DomainUtils.identify(domainObject));
                        invalidatedObjects.add(canonicalObject);
                        canonicalObject = domainObject;
                        updateCaches(id, canonicalObject);
                    } else {
                        log.debug("putOrUpdate({}): Returning cached instance: {}", id, DomainUtils.identify(canonicalObject));
                    }
                } else {
                    canonicalObject = domainObject;
                    log.debug("putOrUpdate{{}}: Caching: {}", id, DomainUtils.identify(canonicalObject));
                    updateCaches(id, canonicalObject);
                }

                canonicalObjects.add(canonicalObject);
            }
        }

        if (!invalidatedObjects.isEmpty()) {
            notifyDomainObjectsInvalidated(invalidatedObjects, invalidateTree);
        }

        return canonicalObjects;
    }

    private void updateCaches(Reference id, DomainObject domainObject) {
        objectCache.put(id, domainObject);
        if (domainObject instanceof Workspace) {
            if (workspaceCache != null) workspaceCache.put(id, (Workspace) domainObject);
        } else if (domainObject instanceof Ontology) {
            if (ontologyCache != null) ontologyCache.put(id, (Ontology) domainObject);
        } else if (domainObject instanceof LineRelease) {
            if (releaseCache != null) releaseCache.put(id, (LineRelease) domainObject);
        } else if (domainObject instanceof ContainerizedService) {
            if (containerCache != null) containerCache.put(id, (ContainerizedService) domainObject);
        }
    }

    /**
     * Clear the entire cache without raising any events. This is basically only useful for changing logins.
     */
    public void invalidateAll() {
        // Invalidating everything causes a lot of updates. We should only do it 
        // if the token is valid. 
        if (AccessManager.getAccessManager().getToken() == null) {
            throw new IllegalStateException("Cannot refresh when token is invalid");
        }
        log.info("Invalidating all objects");
        synchronized (modelLock) {
            this.workspaceCache = null;
            this.ontologyCache = null;
            this.releaseCache = null;
            this.containerCache = null;
            objectCache.invalidateAll();
            cachedSearchResults.invalidateAll();;
        }
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
        if (workspaceCache != null) workspaceCache.remove(ref);
        if (ontologyCache != null) ontologyCache.remove(ref);
        if (releaseCache != null) releaseCache.remove(ref);
        if (containerCache != null) containerCache.remove(ref);

        // Reload the domain object and stick it into the cache
        DomainObject canonicalDomainObject;
        try {
            canonicalDomainObject = loadDomainObject(ref);
            if (canonicalDomainObject == null) {
                log.trace("Cannot reload object which no longer exists: " + ref);
                return;
            }
            canonicalDomainObject = putOrUpdate(canonicalDomainObject);
            log.debug("Got new canonical object: {}", DomainUtils.identify(canonicalDomainObject));
        } catch (Exception e) {
            log.error("Problem reloading invalidated object: {}", DomainUtils.identify(domainObject), e);
        }
    }

    /**
     * Load an object from the database, ignoring the cache.
     *
     * @param ref
     * @return
     */
    private <T extends DomainObject> T loadDomainObject(Reference ref) throws Exception {
        log.debug("loadDomainObject({})", ref);
        return domainFacade.getDomainObject(ref);
    }

    /**
     * Retrieve the given domain object from the database, ignoring the cache.
     *
     * @param domainObject
     * @return canonical domain object instance
     */
    public <T extends DomainObject> T getDomainObject(T domainObject) throws Exception {
        if (domainObject == null) return null;
        return getDomainObject(Reference.createFor(domainObject));
    }

    /**
     * Retrieve the given domain object, checking the cache first and then the database.
     *
     * @param ref
     * @return canonical domain object instance
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public <T extends DomainObject> T getDomainObject(Reference ref) throws Exception {
        log.debug("getDomainObject({})", ref);
        if (ref == null) return null;
        DomainObject domainObject = objectCache.getIfPresent(ref);
        if (domainObject != null) {
            log.debug("getEntityById: returning cached domain object {}", DomainUtils.identify(domainObject));
            return (T) domainObject;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            log.warn("getDomainObject called on EDT for {} which cannot be found in the cache.", ref);
            return null;
        }
        return putOrUpdate((T) loadDomainObject(ref));
    }

    public List<DomainObject> getDomainObjects(List<Reference> references) throws Exception {
        return getDomainObjectsAs(DomainObject.class, references);
    }

    public List<DomainObject> getChildren(Node node, String sortCriteria, int page, int pageSize) throws Exception {
        log.debug("getChildren({}, sortCriteria={}, page={})", node, sortCriteria, page);
        StopWatch w = TIMER ? new LoggingStopWatch() : null;
        List<DomainObject> domainObjects;
        if (node.getId()==null || node.getId()<0) {
            // This is not a persisted node, so we can't use the usual pagination method.
            // Get everything, sort it, and then select the page.
            // TODO: this could be improved by moving this logic to the server-side
            List<DomainObject> allDomainObjects = getDomainObjects(node.getChildren());
            log.info("Looked up {} children and got {} objects", node.getChildren().size(), allDomainObjects.size());
            DomainUtils.sortDomainObjects(allDomainObjects, sortCriteria);
            List<Annotation> annotations = getAnnotations(node.getChildren());
            DomainObjectSearchResults results = new DomainObjectSearchResults(allDomainObjects, annotations);
            log.info("Got results: {}", results);
            DomainObjectResultPage resultPage = results.getPage(page);
            if (resultPage==null) {
                throw new IllegalStateException("Results have no such page: "+page);
            }
            domainObjects = resultPage.getObjects();
        }
        else {
            // This is a persisted node, so we can paginate on the server and return just the requested page
            domainObjects = workspaceFacade.getChildren(node, sortCriteria, page, pageSize);
        }

        List<DomainObject> canonicalObjects = putOrUpdate(domainObjects, false);
        if (TIMER) if (TIMER) w.stop("getDomainObjects(references)");
        log.debug("getDomainObjects: returning {} objects", canonicalObjects.size());
        return canonicalObjects;
    }

    @SuppressWarnings("unchecked")
    public <T extends DomainObject> List<T> getDomainObjectsAs(Class<T> domainClass, List<Reference> references) throws Exception {

        if (references == null) return new ArrayList<>();

        log.debug("getDomainObjects(references.size={})", references.size());

        StopWatch w = TIMER ? new LoggingStopWatch() : null;

        Map<Reference, T> map = new HashMap<>();
        List<Reference> unsatisfiedRefs = new ArrayList<>();

        for (Reference ref : references) {
            T domainObject = ref == null ? null : (T) objectCache.getIfPresent(ref);
            if (domainObject != null) {
                map.put(ref, domainObject);
            } else if (ref != null) {
                unsatisfiedRefs.add(ref);
            }
        }

        if (!unsatisfiedRefs.isEmpty()) {
            List<DomainObject> objects = domainFacade.getDomainObjects(unsatisfiedRefs);
            List<T> classObjects = new ArrayList<>();
            for (DomainObject domainObject : objects) {
                classObjects.add((T) domainObject);
            }
            map.putAll(DomainUtils.getMapByReference(classObjects));
        }

        unsatisfiedRefs.clear();

        List<T> domainObjects = new ArrayList<>();
        for (Reference ref : references) {
            T domainObject = map.get(ref);
            if (domainObject != null) {
                domainObjects.add(domainObject);
            } else {
                unsatisfiedRefs.add(ref);
            }
        }

        List<T> canonicalObjects = putOrUpdate(domainObjects, false);
        if (TIMER) if (TIMER) w.stop("getDomainObjects(references)");
        log.debug("getDomainObjects: returning {} objects ({} unsatisfied)", canonicalObjects.size(), unsatisfiedRefs.size());
        return canonicalObjects;
    }

    public <T extends DomainObject> T getDomainObject(Class<T> domainClass, Long id) throws Exception {
        List<T> list = getDomainObjects(domainClass, Collections.singletonList(id));
        return list.isEmpty() ? null : list.get(0);
    }

    @SuppressWarnings("unchecked")
    public <T extends DomainObject> List<T> getDomainObjects(Class<T> clazz, List<Long> ids) throws Exception {
        List<T> objects = new ArrayList<>();
        for (DomainObject domainObject : getDomainObjects(clazz.getSimpleName(), ids)) {
            objects.add((T) domainObject);
        }
        return objects;
    }

    public DomainObject getDomainObject(String className, Long id) throws Exception {
        List<DomainObject> objects = getDomainObjects(className, Collections.singletonList(id));
        return objects.isEmpty() ? null : objects.get(0);
    }

    public <T extends DomainObject> List<T> getDomainObjects(Class<T> domainClass, String name) throws Exception {
        List<T> objects = new ArrayList<>();
        StopWatch w = TIMER ? new LoggingStopWatch() : null;
        objects.addAll(domainFacade.getDomainObjects(domainClass, name));
        if (TIMER) w.stop("getDomainObjects(Class,objectName)");
        return objects;
    }

    public <T extends DomainObject> List<T> getDomainObjects(String className, List<Long> ids) throws Exception {

        if (className == null || ids == null) return new ArrayList<>();
        log.debug("getDomainObjects(ids.size={})", ids.size());

        StopWatch w = TIMER ? new LoggingStopWatch() : null;

        Map<Reference, T> map = new HashMap<>();
        List<Long> unsatisfiedIds = new ArrayList<>();

        for (Long id : ids) {
            Reference ref = Reference.createFor(className, id);
            @SuppressWarnings("unchecked")
            T domainObject = (T) objectCache.getIfPresent(ref);
            if (domainObject != null) {
                map.put(ref, domainObject);
            } else {
                unsatisfiedIds.add(id);
            }
        }

        if (!unsatisfiedIds.isEmpty()) {
            List<T> objects = domainFacade.getDomainObjects(className, unsatisfiedIds);
            map.putAll(DomainUtils.getMapByReference(objects));
        }

        unsatisfiedIds.clear();

        List<T> domainObjects = new ArrayList<>();
        for (Long id : ids) {
            Reference ref = Reference.createFor(className, id);
            T domainObject = map.get(ref);
            if (domainObject != null) {
                domainObjects.add(domainObject);
            } else {
                unsatisfiedIds.add(id);
            }
        }

        List<T> canonicalObjects = putOrUpdate(domainObjects, false);
        if (TIMER) w.stop("getDomainObjects(className,ids)");
        log.debug("getDomainObjects: returning {} objects ({} unsatisfied)", canonicalObjects.size(), unsatisfiedIds.size());
        return canonicalObjects;
    }

    @SuppressWarnings("unchecked")
    public <T extends DomainObject> List<T> getAllDomainObjectsByClass(Class<T> clazz) throws Exception {
        return (List<T>) getAllDomainObjectsByClass(clazz.getName());
    }

    public List<DomainObject> getAllDomainObjectsByClass(String className) throws Exception {
        List<DomainObject> domainObjects = new ArrayList<>();
        if (className == null) return domainObjects;
        log.debug("getAllDomainObjectsByClass({})", className);

        StopWatch w = TIMER ? new LoggingStopWatch() : null;
        domainObjects = domainFacade.getAllDomainObjectsByClass(className);

        if (TIMER) w.stop("getAllDomainObjectsByClass()");
        log.debug("getAllDomainObjectsByClass: returning {} objects", domainObjects.size());
        return putOrUpdate(domainObjects);
    }

    public List<DomainObject> getDomainObjects(ReverseReference reverseReference) throws Exception {
        List<DomainObject> domainObjects = domainFacade.getDomainObjects(reverseReference);
        return putOrUpdate(domainObjects, false);
    }

    @SuppressWarnings("unchecked")
    public <T extends DomainObject> List<T> getDomainObjectsAs(Class<T> clazz, ReverseReference reverseReference) throws Exception {
        List<DomainObject> domainObjects = domainFacade.getDomainObjects(reverseReference);
        List<T> asClass = new ArrayList<>();
        for (DomainObject domainObject : domainObjects) {
            asClass.add((T) domainObject);
        }
        return putOrUpdate(asClass, false);
    }

    public List<Annotation> getAnnotations(DomainObject domainObject) throws Exception {
        return getAnnotations(Reference.createFor(domainObject));
    }

    public List<Annotation> getAnnotations(Reference reference) throws Exception {
        return getAnnotations(Collections.singletonList(reference));
    }

    public List<Annotation> getAnnotations(Collection<Reference> references) throws Exception {
        if (references == null) return new ArrayList<>();
        StopWatch w = TIMER ? new LoggingStopWatch() : null;
        List<Annotation> annotations = ontologyFacade.getAnnotations(references);
        // TODO: cache the annotations?
        if (TIMER) w.stop("getAnnotations(references)");
        return annotations;
    }

    public List<Workspace> getWorkspaces() throws Exception {
        Collection<Workspace> values;
        synchronized (modelLock) {
            if (workspaceCache == null) {
                log.info("Caching workspaces from database...");
                this.workspaceCache = new LinkedHashMap<>();
                StopWatch w = TIMER ? new LoggingStopWatch() : null;
                Collection<Workspace> workspaces = workspaceFacade.getWorkspaces();
                List<Workspace> canonicalObjects = putOrUpdate(workspaces, false);
                for (Workspace workspace : canonicalObjects) {
                    log.info("Caching workspace: {} ({})", workspace.getName(), workspace.getOwnerKey());
                    workspaceCache.put(Reference.createFor(workspace), workspace);
                }
                if (TIMER) w.stop("getWorkspaces");
            }
            values = workspaceCache.values();
        }
        return new ArrayList<>(values);
    }

    public Workspace getDefaultWorkspace() throws Exception {
        for (Workspace workspace : getWorkspaces()) {
            if (workspace.getOwnerKey().equals(AccessManager.getSubjectKey()) && DomainConstants.NAME_DEFAULT_WORKSPACE.equals(workspace.getName())) {
                return workspace;
            }
        }
        throw new IllegalStateException("Cannot find default workspace");
    }

    public TreeNode getDefaultWorkspaceFolder(String folderName) throws Exception {
        return getDefaultWorkspaceFolder(folderName, false);
    }

    public TreeNode getDefaultWorkspaceFolder(String folderName, boolean createIfNecessary) throws Exception {
        Workspace workspace = getDefaultWorkspace();
        List<DomainObject> children = getDomainObjects(workspace.getChildren());
        TreeNode treeNode = DomainUtils.findObjectByTypeAndName(children, TreeNode.class, folderName);
        if (treeNode == null && createIfNecessary) {
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
        dataSets.sort(new DataSetComparator());
        List<DataSet> canonicalDataSets = putOrUpdate(dataSets, false);
        if (TIMER) w.stop("getDataSets");
        return canonicalDataSets;
    }

    public static class DataSetComparator implements Comparator<DataSet> {
        @Override
        public int compare(DataSet o1, DataSet o2) {
            return ComparisonChain.start()
                    .compareTrueFirst(ClientDomainUtils.isOwner(o1), ClientDomainUtils.isOwner(o2))
                    .compare(o1.getIdentifier(), o2.getIdentifier()).result();
        }
    }

    public DataSet getDataSet(String identifier) throws Exception {
        // TODO: improve performance by fetching only the required data set
        for (DataSet dataSet : getDataSets()) {
            if (dataSet.getIdentifier().equals(identifier)) {
                return dataSet;
            }
        }
        return null;
    }

    public List<EMDataSet> getEMDataSets() throws Exception {
        StopWatch w = TIMER ? new LoggingStopWatch() : null;
        List<EMDataSet> dataSets = getAllDomainObjectsByClass(EMDataSet.class);
        dataSets.sort(new EMDataSetComparator());
        List<EMDataSet> canonicalDataSets = putOrUpdate(dataSets, false);
        if (TIMER) w.stop("getDataSets");
        return canonicalDataSets;
    }

    public static class EMDataSetComparator implements Comparator<EMDataSet> {
        @Override
        public int compare(EMDataSet o1, EMDataSet o2) {
            return ComparisonChain.start()
                    .compareTrueFirst(ClientDomainUtils.isOwner(o1), ClientDomainUtils.isOwner(o2))
                    .compare(o1.getDataSetIdentifier(), o2.getDataSetIdentifier()).result();
        }
    }

    public List<ColorDepthLibrary> getColorDepthLibraries() throws Exception {
        StopWatch w = TIMER ? new LoggingStopWatch() : null;
        List<ColorDepthLibrary> libraries = new ArrayList<>();
        for (DomainObject obj : domainFacade.getAllDomainObjectsByClass(ColorDepthLibrary.class.getName())) {
            if (obj instanceof ColorDepthLibrary) {
                libraries.add((ColorDepthLibrary)obj);
            }
        }
        libraries.sort(new ColorDepthComparator());
        List<ColorDepthLibrary> canonicalLibraries = putOrUpdate(libraries, false);
        if (TIMER) w.stop("getColorDepthLibraries");
        return canonicalLibraries;
    }

    public List<ColorDepthLibrary> getColorDepthLibraries(String alignmentSpace) throws Exception {
        StopWatch w = TIMER ? new LoggingStopWatch() : null;
        List<ColorDepthLibrary> libraries = new ArrayList<>(sampleFacade.getColorDepthLibraries(alignmentSpace));
        libraries.sort(new ColorDepthComparator());
        List<ColorDepthLibrary> canonicalLibraries = putOrUpdate(libraries, false);
        if (TIMER) w.stop("getColorDepthLibraries");
        return canonicalLibraries;
    }

    public class ColorDepthComparator implements Comparator<ColorDepthLibrary> {
        @Override
        public int compare(ColorDepthLibrary o1, ColorDepthLibrary o2) {
            return ComparisonChain.start()
                    .compareTrueFirst(ClientDomainUtils.isOwner(o1), ClientDomainUtils.isOwner(o2))
                    .compare(o1.getIdentifier(), o2.getIdentifier()).result();
        }
    };

    public List<String> getAlignmentSpaces() throws Exception {
        StopWatch w = TIMER ? new LoggingStopWatch() : null;
        Set<String> alignmentSpaces = new TreeSet<>();
        for (ColorDepthLibrary library : getColorDepthLibraries()) {
            for (String alignmentSpace : library.getColorDepthCounts().keySet()) {
                if (ColorDepthUtils.isAlignmentSpaceVisible(alignmentSpace)) {
                    alignmentSpaces.addAll(library.getColorDepthCounts().keySet());
                }
            }
        }
        if (TIMER) w.stop("getAlignmentSpaces");
        return new ArrayList<>(alignmentSpaces);
    }

    public List<LSMImage> getLsmsForSample(Sample sample) throws Exception {
        StopWatch w = TIMER ? new LoggingStopWatch() : null;
        List<Reference> lsmRefs = new ArrayList<>();
        for (ObjectiveSample objectiveSample : sample.getObjectiveSamples()) {
            for (SampleTile tile : objectiveSample.getTiles()) {
                lsmRefs.addAll(tile.getLsmReferences());
            }
        }
        List<LSMImage> images = getDomainObjectsAs(LSMImage.class, lsmRefs);
        if (TIMER) w.stop("getLsmsForSample");
        return images;
    }

    public List<Reference> getContainerReferences(DomainObject object) {
        StopWatch w = TIMER ? new LoggingStopWatch() : null;
        try {
            return workspaceFacade.getContainerReferences(object);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            if (TIMER) w.stop("getContainerReferences");
        }
    }

    public void remove(List<? extends DomainObject> domainObjects) throws Exception {
        domainFacade.remove(DomainUtils.getReferences(domainObjects));
        invalidate(domainObjects);
        for (DomainObject object : domainObjects) {
            notifyDomainObjectRemoved(object);
        }
    }

    public void removeStorage(List<String> storagePaths) {
        for (String path : storagePaths) {
            log.info("Removing object storage path: {}", path);
        }
        domainFacade.removeObjectStorage(storagePaths);
    }

    /**
     * Returns all of the containerized services in the system.
     *
     * @return collection of containerized services
     */
    public List<ContainerizedService> getContainerizedServices() {
        synchronized (modelLock) {
            if (containerCache == null) {
                log.info("Getting containerized services from database");
                this.containerCache = new LinkedHashMap<>();
                StopWatch w = TIMER ? new LoggingStopWatch() : null;
                try {
                    Collection<ContainerizedService> services = getAllDomainObjectsByClass(ContainerizedService.class);
                    List<ContainerizedService> canonicalObjects = putOrUpdate(services, false);
                    for (ContainerizedService service : canonicalObjects) {
                        containerCache.put(Reference.createFor(service), service);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Could not retrieve containerized services", e);
                }
                if (TIMER) w.stop("getContainerizedServices");
            }
        }
        return new ArrayList<>(containerCache.values());
    }

    /**
     * Returns all of the ontologies that a user has access to view.
     *
     * @return collection of ontologies
     */
    public List<Ontology> getOntologies() {
        Collection<Ontology> values;
        synchronized (modelLock) {
            if (ontologyCache == null) {
                log.debug("Getting ontologies from database");
                this.ontologyCache = new LinkedHashMap<>();
                StopWatch w = TIMER ? new LoggingStopWatch() : null;
                Collection<Ontology> ontologies = ontologyFacade.getOntologies();
                List<Ontology> canonicalObjects = putOrUpdate(ontologies, false);
                for (Ontology ontology : canonicalObjects) {
                    ontologyCache.put(Reference.createFor(ontology), ontology);
                }
                if (TIMER) w.stop("getOntologies");
            }
            values = ontologyCache.values();
        }
        return new ArrayList<>(values);
    }

    /**
     * Returns an ordered set of all the unique terms in an ontology tree, rooted at the given term.
     *
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
     *
     * @param reference a reference to an ontology term
     * @return the ontology term
     */
    public OntologyTerm getOntologyTermByReference(OntologyTermReference reference) throws Exception {
        if (reference == null || reference.getOntologyId() == null) {
            return null;
        }
        Ontology ontology = getDomainObject(Ontology.class, reference.getOntologyId());
        return findTerm(ontology, reference.getOntologyTermId());
    }

    private OntologyTerm findTerm(OntologyTerm term, Long termId) {
        if (term == null) {
            return null;
        }
        if (term.getId().equals(termId)) {
            return term;
        }
        if (term.getTerms() != null) {
            for (OntologyTerm child : term.getTerms()) {
                OntologyTerm found = findTerm(child, termId);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    public Ontology create(Ontology ontology) throws Exception {
        Ontology canonicalObject;
        synchronized (modelLock) {
            canonicalObject = putOrUpdate(ontologyFacade.create(ontology));
            if (ontologyCache != null) ontologyCache.put(Reference.createFor(canonicalObject), canonicalObject);
        }
        notifyDomainObjectCreated(canonicalObject);
        return canonicalObject;
    }

    public Ontology reorderOntologyTerms(Long ontologyId, Long parentTermId, int[] order) throws Exception {
        Ontology canonicalObject;
        synchronized (modelLock) {
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
        synchronized (modelLock) {
            canonicalObject = putOrUpdate(ontologyFacade.addTerms(ontologyId, parentTermId, terms, index));
        }
        notifyDomainObjectChanged(canonicalObject);
        return canonicalObject;
    }

    public Ontology removeOntologyTerm(Long ontologyId, Long parentTermId, Long termId) throws Exception {
        Ontology canonicalObject;
        synchronized (modelLock) {
            canonicalObject = putOrUpdate(ontologyFacade.removeTerm(ontologyId, parentTermId, termId));
        }
        notifyDomainObjectChanged(canonicalObject);
        return canonicalObject;
    }

    public void removeOntology(Long ontologyId) throws Exception {
        Ontology canonicalObject = getDomainObject(Ontology.class, ontologyId);
        synchronized (modelLock) {
            ontologyFacade.removeOntology(ontologyId);
            if (ontologyCache != null) ontologyCache.remove(Reference.createFor(canonicalObject));
        }
        notifyDomainObjectRemoved(canonicalObject);
    }

    public TreeNode create(TreeNode treeNode) throws Exception {
        TreeNode canonicalObject;
        synchronized (modelLock) {
            canonicalObject = putOrUpdate(workspaceFacade.create(treeNode));
        }
        notifyDomainObjectCreated(canonicalObject);
        return canonicalObject;
    }

    public Filter save(Filter filter) throws Exception {
        boolean create = filter.getId() == null;
        Filter canonicalObject;
        synchronized (modelLock) {
            canonicalObject = putOrUpdate(create ? workspaceFacade.create(filter) : workspaceFacade.update(filter));
        }
        if (create) {
            notifyDomainObjectCreated(canonicalObject);
        } else {
            notifyDomainObjectChanged(canonicalObject);
        }
        return canonicalObject;
    }

    public DocumentSearchResults search(DocumentSearchParams query) throws Exception {
        StopWatch w = TIMER ? new LoggingStopWatch() : null;
        DocumentSearchResults results = cachedSearchResults.get(query);
        if (TIMER) w.stop("search(query)");
        return results;
    }

    public DataSet save(DataSet dataSet) throws Exception {
        DataSet canonicalObject;
        boolean create = dataSet.getId() == null;
        synchronized (modelLock) {
            canonicalObject = putOrUpdate(create ? sampleFacade.create(dataSet) : sampleFacade.update(dataSet));
        }
        if (create) {
            notifyDomainObjectCreated(canonicalObject);
        } else {
            notifyDomainObjectChanged(canonicalObject);
        }
        return canonicalObject;
    }

    public void remove(DataSet dataSet) throws Exception {
        sampleFacade.remove(dataSet);
        notifyDomainObjectRemoved(dataSet);
    }

    public Annotation createAnnotation(Reference target, OntologyTermReference ontologyTermReference, String value) throws Exception {
        Annotation canonicalObject;
        synchronized (modelLock) {
            canonicalObject = putOrUpdate(ontologyFacade.createAnnotation(target, ontologyTermReference, value));
        }
        notifyAnnotationsChanged(getDomainObject(canonicalObject.getTarget()));
        return canonicalObject;
    }

    public Annotation updateAnnotation(Annotation annotation, String newValue) throws Exception {
        Annotation canonicalObject;
        synchronized (modelLock) {
            canonicalObject = putOrUpdate(ontologyFacade.updateAnnotation(annotation, newValue));
        }
        notifyAnnotationsChanged(getDomainObject(annotation.getTarget()));
        return canonicalObject;
    }

    public void removeAnnotation(Annotation annotation) throws Exception {
        ontologyFacade.removeAnnotation(annotation);
        notifyAnnotationsChanged(getDomainObject(annotation.getTarget()));
    }

    public <T extends Node> T reorderChildren(T node, int[] order) throws Exception {
        T canonicalObject;
        synchronized (modelLock) {
            canonicalObject = putOrUpdate(workspaceFacade.reorderChildren(node, order));
        }
        notifyDomainObjectChanged(canonicalObject);
        return canonicalObject;
    }

    public <T extends Node> T addChild(T node, DomainObject domainObject) throws Exception {
        return addChildren(node, Collections.singletonList(domainObject));
    }

    public <T extends Node> T addChild(T node, DomainObject domainObject, Integer index) throws Exception {
        return addChildren(node, Collections.singletonList(domainObject), index);
    }

    public <T extends Node> T addChildren(T node, Collection<? extends DomainObject> domainObjects) throws Exception {
        return addChildren(node, domainObjects, null);
    }

    public <T extends Node> T addChildren(T node, Collection<? extends DomainObject> domainObjects, Integer index) throws Exception {
        T canonicalObject;
        synchronized (modelLock) {
            canonicalObject = putOrUpdate(workspaceFacade.addChildren(node, DomainUtils.getReferences(domainObjects), index));
        }
        notifyDomainObjectChanged(canonicalObject);
        return canonicalObject;
    }

    public <T extends Node> T removeChild(T node, DomainObject domainObject) throws Exception {
        return removeChildren(node, Collections.singletonList(domainObject));
    }

    public <T extends Node> T removeChildren(T node, Collection<? extends DomainObject> domainObjects) throws Exception {
        T canonicalObject;
        synchronized (modelLock) {
            canonicalObject = putOrUpdate(workspaceFacade.removeChildren(node, DomainUtils.getReferences(domainObjects)));
        }
        notifyDomainObjectChanged(canonicalObject);
        return canonicalObject;
    }

    public <T extends Node> T removeReference(T node, Reference reference) throws Exception {
        T canonicalObject;
        synchronized (modelLock) {
            canonicalObject = putOrUpdate(workspaceFacade.removeChildren(node, Arrays.asList(reference)));
        }
        return canonicalObject;
    }

    public List<LineRelease> getLineReleases() throws Exception {
        Collection<LineRelease> values;
        synchronized (modelLock) {
            if (releaseCache == null) {
                log.debug("Getting line releases from database");
                this.releaseCache = new LinkedHashMap<>();
                StopWatch w = TIMER ? new LoggingStopWatch() : null;
                List<LineRelease> releases = sampleFacade.getLineReleases();
                List<LineRelease> canonicalObjects = putOrUpdate(releases, false);
                for (LineRelease lineRelease : canonicalObjects) {
                    releaseCache.put(Reference.createFor(lineRelease), lineRelease);
                }
                if (TIMER) w.stop("getLineReleases");
            }
            values = releaseCache.values();
        }
        return new ArrayList<>(values);
    }

    public List<LineRelease> getLineReleases(Sample sample) throws Exception {
        // Find which releases these samples belong to
        Reference sampleRef = Reference.createFor(sample);
        return getLineReleases()
                .stream()
                .filter(r -> r.getChildren().stream().anyMatch(sampleRef::equals))
                .sorted(Comparator.comparing(AbstractDomainObject::getName))
                .collect(Collectors.toList());
    }

    public LineRelease createLineRelease(String name) throws Exception {
        LineRelease canonicalObject;
        synchronized (modelLock) {
            canonicalObject = putOrUpdate(sampleFacade.createLineRelease(name));
            if (releaseCache != null) releaseCache.put(Reference.createFor(canonicalObject), canonicalObject);
        }
        notifyDomainObjectCreated(canonicalObject);
        return canonicalObject;
    }

    public LineRelease update(LineRelease release) throws Exception {
        LineRelease canonicalObject;
        synchronized (modelLock) {
            canonicalObject = putOrUpdate(sampleFacade.update(release));
            if (releaseCache != null) releaseCache.put(Reference.createFor(canonicalObject), canonicalObject);
        }
        notifyDomainObjectChanged(canonicalObject);
        return canonicalObject;
    }

    public void remove(LineRelease release) throws Exception {
        synchronized (modelLock) {
            sampleFacade.remove(release);
            if (ontologyCache != null) ontologyCache.remove(Reference.createFor(release));
        }
        notifyDomainObjectRemoved(release);
    }

    @SuppressWarnings("unchecked")
    public <T extends DomainObject> T save(T domainObject) throws Exception {
        T canonicalObject;
        synchronized (modelLock) {
            canonicalObject = putOrUpdate((T) domainFacade.save(domainObject));
        }
        log.info("Saved {} as {}", domainObject, canonicalObject);
        if (domainObject.getId() == null) {
            notifyDomainObjectCreated(canonicalObject);
            notifyDomainObjectChanged(canonicalObject); // Backwards compatibility until we can change everything to respond to creation events
        } else {
            notifyDomainObjectChanged(canonicalObject);
        }
        return canonicalObject;
    }

    public <T extends DomainObject> T updateProperty(T domainObject, String propName, Object propValue) throws Exception {
        T canonicalObject;
        synchronized (modelLock) {
            canonicalObject = putOrUpdate((T) domainFacade.updateProperty(domainObject, propName, propValue));
        }
        notifyDomainObjectChanged(canonicalObject);
        return canonicalObject;
    }

    public <T extends DomainObject> T changePermissions(T domainObject, String granteeKey, String rights) throws Exception {
        T canonicalObject;
        synchronized (modelLock) {
            canonicalObject = putOrUpdate((T) domainFacade.setPermissions(domainObject, granteeKey, rights), true);
        }
        notifyDomainObjectChanged(canonicalObject);
        return canonicalObject;
    }

    public Subject getSubjectByNameOrKey(String subjectKey) throws Exception {
        return subjectFacade.getSubjectByNameOrKey(subjectKey);
    }

    public Subject getUser(String username) throws Exception {
        return subjectFacade.getUser(username);
    }

    public String dispatchSamples(SampleReprocessingRequest request) throws Exception {
        return sampleFacade.dispatchSamples(request);
    }

    public Long dispatchTask(JsonTask task, String processName) throws Exception {
        return sampleFacade.dispatchTask(task, processName);
    }

    public ColorDepthSearch createColorDepthSearch(ColorDepthSearch search) throws Exception {
        search = save(search);

        TreeNode searchesFolder = getDefaultWorkspaceFolder(DomainConstants.NAME_COLOR_DEPTH_SEARCHES, true);
        addChild(searchesFolder, search);

        return search;
    }

    public ColorDepthMask createColorDepthMask(String maskName, String alignmentSpace, String filepath, Integer maskThreshold, Sample sample, String anatomicalArea) throws Exception {

        ColorDepthMask mask = new ColorDepthMask();
        mask.setName(maskName);
        mask.setAlignmentSpace(alignmentSpace);
        mask.setAnatomicalArea(anatomicalArea);
        mask.setFilepath(filepath);
        mask.setMaskThreshold(maskThreshold);
        if (sample != null) {
            mask.setSample(Reference.createFor(sample));
        }
        mask = save(mask);

        log.info("Saved {} with area={} and alignmentSpace={}", mask, anatomicalArea, alignmentSpace);

        // Add it to the mask folder
        TreeNode masksFolder = getDefaultWorkspaceFolder(DomainConstants.NAME_COLOR_DEPTH_MASKS, true);
        addChild(masksFolder, mask);

        return mask;
    }

    public ColorDepthSearch addMaskToSearch(ColorDepthSearch search, ColorDepthMask mask) throws Exception {
        Reference ref = Reference.createFor(mask);
        if (search.getMasks().contains(ref)) return search;
        search.getParameters().addMask(ref);
        return save(search);
    }

    public ColorDepthSearch removeMaskFromSearch(ColorDepthSearch search, ColorDepthMask mask) throws Exception {
        Reference ref = Reference.createFor(mask);
        if (search.getMasks().contains(ref)) {
            search.getMasks().remove(ref);
            return save(search);
        }
        throw new IllegalArgumentException(mask + " was not found within " + search);
    }


    // EVENT HANDLING 
    // Important: never call these methods from within a synchronized. That can lead to deadlocks because
    // the event bus is also synchronized, and events can trigger domain model access.

    private boolean notify = true;

    public boolean isNotify() {
        return notify;
    }

    public void setNotify(boolean notify) {
        this.notify = notify;
    }

    public void notifyDomainObjectCreated(DomainObject domainObject) {
        if (!notify) return;
        if (log.isTraceEnabled()) {
            log.trace("Generating DomainObjectCreateEvent for {}", DomainUtils.identify(domainObject));
        }
        if (domainObject == null) {
            throw new IllegalStateException("Cannot notify creation of null object");
        }
        Events.getInstance().postOnEventBus(new DomainObjectCreateEvent(domainObject));
    }

    public void notifyDomainObjectChanged(DomainObject domainObject) {
        if (!notify) return;
        if (log.isTraceEnabled()) {
            log.trace("Generating DomainObjectChangeEvent for {}", DomainUtils.identify(domainObject));
        }
        if (domainObject == null) {
            throw new IllegalStateException("Cannot notify change of null object");
        } else {
            Events.getInstance().postOnEventBus(new DomainObjectChangeEvent(domainObject));
        }
    }

    public void notifyAnnotationsChanged(DomainObject domainObject) {
        if (!notify) return;
        if (log.isTraceEnabled()) {
            log.trace("Generating DomainObjectAnnotationChangeEvent for {}", DomainUtils.identify(domainObject));
        }
        if (domainObject == null) {
            throw new IllegalStateException("Cannot notify annotation change of null object");
        }
        Events.getInstance().postOnEventBus(new DomainObjectAnnotationChangeEvent(domainObject));
    }

    public void notifyDomainObjectRemoved(DomainObject domainObject) {
        if (!notify) return;
        if (log.isTraceEnabled()) {
            log.trace("Generating DomainObjectRemoveEvent for {}", DomainUtils.identify(domainObject));
        }
        if (domainObject == null) {
            throw new IllegalStateException("Cannot notify removal of null object");
        }
        Events.getInstance().postOnEventBus(new DomainObjectRemoveEvent(domainObject));
    }

    private void notifyDomainObjectsInvalidated(Collection<? extends DomainObject> objects, boolean invalidateTree) {
        if (!notify) return;
        if (log.isTraceEnabled()) {
            log.trace("Generating DomainObjectInvalidationEvent with {} entities", objects.size());
        }

        Collection<DomainObject> invalidated = new ArrayList<>();
        if (invalidateTree) {
            for (DomainObject domainObject : objects) {
                addTree(invalidated, domainObject);
            }
        } else {
            invalidated.addAll(objects);
        }

        if (!invalidated.isEmpty()) {
            Events.getInstance().postOnEventBus(new DomainObjectInvalidationEvent(invalidated));
        }
    }

    private void addTree(Collection<DomainObject> objects, DomainObject domainObject) {

        if (domainObject instanceof TreeNode) {
            TreeNode treeNode = (TreeNode) domainObject;
            for (Reference childRef : treeNode.getChildren()) {
                DomainObject childObj = objectCache.getIfPresent(childRef);
                if (childObj != null) {
                    objectCache.invalidate(childRef);
                    addTree(objects, childObj);
                }
            }
        } else if (domainObject instanceof Sample) {
            // TODO: Should handle invalidation of LSMs and neuron fragments
        }

        objects.add(domainObject);
    }
}
