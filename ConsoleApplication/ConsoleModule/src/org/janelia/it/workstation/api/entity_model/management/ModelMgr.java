package org.janelia.it.workstation.api.entity_model.management;

import java.awt.Color;
import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executor;

import javax.swing.SwingUtilities;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import org.apache.solr.client.solrj.SolrQuery;
import org.janelia.it.jacs.compute.api.support.MappedId;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityActorPermission;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.annotation.AnnotationSessionTask;
import org.janelia.it.jacs.model.tasks.utility.ContinuousExecutionTask;
import org.janelia.it.jacs.model.tasks.utility.GenericTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.user_data.UserToolEvent;
import org.janelia.it.jacs.model.user_data.prefs.SubjectPreference;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.CoordinateToRawTransform;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.RawFileInfo;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmSample;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmWorkspace;
import org.janelia.it.jacs.shared.annotation.DataDescriptor;
import org.janelia.it.jacs.shared.annotation.DataFilter;
import org.janelia.it.jacs.shared.annotation.FilterResult;
import org.janelia.it.jacs.shared.solr.SageTerm;
import org.janelia.it.jacs.shared.solr.SolrResults;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.api.entity_model.access.ModelMgrObserver;
import org.janelia.it.workstation.api.entity_model.fundtype.TaskFilter;
import org.janelia.it.workstation.api.entity_model.fundtype.TaskRequest;
import org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBEntityFacade.EJBLookupException;
import org.janelia.it.workstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.workstation.api.facade.roles.ExceptionHandler;
import org.janelia.it.workstation.api.stub.data.NoDataException;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.model.entity.RootedEntity;
import org.janelia.it.workstation.model.utils.AnnotationSession;
import org.janelia.it.workstation.shared.exception_handlers.PrintStackTraceHandler;
import org.janelia.it.workstation.shared.util.ThreadQueue;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ModelMgr {

    private static final Logger log = LoggerFactory.getLogger(ModelMgr.class);

    public static final String CATEGORY_SORT_CRITERIA = "SortCriteria:";

    private static final ModelMgr modelManager = new ModelMgr();
    private ThreadQueue threadQueue;
    private ThreadQueue notificationQueue;

    private final EventBus modelEventBus;
    private final EntityModel entityModel;
    private final EntitySelectionModel entitySelectionModel;
    private final UserColorMapping userColorMapping;

    private AnnotationSession annotationSession;
    private OntologyAnnotation currentSelectedOntologyAnnotation;

    public Entity ERROR_ONTOLOGY = null;

    private final List<ModelMgrObserver> modelMgrObservers = new ArrayList<>();

    private Long currWorkspaceId;

    static {
        // Register an exception handler.
        ModelMgr.getModelMgr().registerExceptionHandler(new PrintStackTraceHandler());
    }
    
    private final Map<String,Subject> subjectByKey = new HashMap<>();

    private ModelMgr() {
        log.info("Initializing Model Manager");
        // Sync block may/may not be necessary. Problem may just be intermittent.
        //   Saw NPE on use of modelEventBus, during attempt to register against it.
        synchronized (ModelMgr.class) {
            log.debug("ModelMgr c'tor from  " + Thread.currentThread().getClass().getClassLoader() + "/" + Thread.currentThread().getContextClassLoader() + " in thread " + Thread.currentThread());

            this.entityModel = new EntityModel();
            this.entitySelectionModel = new EntitySelectionModel();
            this.userColorMapping = new UserColorMapping();
            this.modelEventBus = new AsyncEventBus("awt", new Executor() {
                public void execute(Runnable cmd) {
                    if (EventQueue.isDispatchThread()) {
                        cmd.run();
                    }
                    else {
                        // TODO: this should queue the command on a queue that is aware of entity invalidation, 
                        // and does not generate other events for an entity if an invalidation is coming. 
                        // This will elimiante the "Instance mismatch" issues that we sometimes have.
                        EventQueue.invokeLater(cmd);
                    }
                }
            });

        }
        log.debug("Successfully initialized");
    } //Singleton enforcement

    public static synchronized ModelMgr getModelMgr() {
        return modelManager;
    }
    
    public void init() throws Exception {
        log.info("Preloading subjects");
        List<Subject> subjects = getSubjects();
        for(Subject subject : subjects) {
            subjectByKey.put(subject.getKey(), subject);
        }
    }

    /**
     * This adds a session event.  It is best to use the method in the
     * Session Manager instead, as that has more convenient 'finding' of various
     * parts of the event.
     *
     * @param event to log.
     */
    public void addEventToSession(UserToolEvent event) {
        try {
            FacadeManager.getFacadeManager().getComputeFacade().addEventToSession(event);
        } catch (Exception ex) {
            log.warn("Failed to log userevent " + event);
            ex.printStackTrace();
        }
    }
    
    /**
     * This adds a session event. It is best to use the method in the Session
     * Manager instead, as that has more convenient 'finding' of various parts
     * of the event.
     *
     * @param event to log.
     */
    public void addEventsToSession(UserToolEvent[] events) {
        try {
            FacadeManager.getFacadeManager().getComputeFacade().addEventsToSession(events);
        } catch (Exception ex) {
            log.warn("Failed to log batch of " + events.length + " user events.");
            ex.printStackTrace();
        }
    }

    public void addModelMgrObserver(ModelMgrObserver mml) {
        if (null != mml && !modelMgrObservers.contains(mml)) {
            modelMgrObservers.add(mml);
        }
    }

    public void removeModelMgrObserver(ModelMgrObserver mml) {
        if (null != mml && modelMgrObservers.contains(mml)) {
            modelMgrObservers.remove(mml);
        }
    }

    public List<ModelMgrObserver> getModelMgrObservers() {
        return new ArrayList<>(modelMgrObservers);
    }

    public void registerExceptionHandler(ExceptionHandler handler) {
        FacadeManager.registerExceptionHandler(handler);
    }

    public void deregisterExceptionHandler(ExceptionHandler handler) {
        FacadeManager.deregisterExceptionHandler(handler);
    }

    public void initErrorOntology() {
        SimpleWorker worker = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                ERROR_ONTOLOGY = FacadeManager.getFacadeManager().getOntologyFacade().getErrorOntology();
            }

            @Override
            protected void hadSuccess() {
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        worker.execute();
    }

    public void handleException(Throwable throwable) {
        if (throwable instanceof NoDataException) {
            return;
        }
        FacadeManager.handleException(throwable);
    }

    public UserColorMapping getUserColorMapping() {
        return userColorMapping;
    }

    public AnnotationSession getCurrentAnnotationSession() {
        return annotationSession;
    }

    public void setCurrentAnnotationSession(AnnotationSession session) {
        if (annotationSession == null || session == null || !annotationSession.getId().equals(session.getId())) {
            this.annotationSession = session;
            if (annotationSession == null) {
                notifyAnnotationSessionDeselected();
            }
            else {
                notifyAnnotationSessionSelected(annotationSession.getId());
            }
        }
    }

    public OntologyAnnotation getCurrentSelectedOntologyAnnotation() {
        return currentSelectedOntologyAnnotation;
    }

    public void setCurrentSelectedOntologyAnnotation(OntologyAnnotation currentSelectedOntologyAnnotation) {
        this.currentSelectedOntologyAnnotation = currentSelectedOntologyAnnotation;
    }

    public Long getCurrentOntologyId() {
        String lastSelectedOntology = (String) SessionMgr.getSessionMgr().getModelProperty("lastSelectedOntology");
        if (StringUtils.isEmpty(lastSelectedOntology)) {
            return null;
        }
        log.debug("Current ontology is {}", lastSelectedOntology);
        return Long.parseLong(lastSelectedOntology);
    }

    public Entity getCurrentOntology() {
        return SessionMgr.getBrowser().getOntologyOutline().getCurrentOntology();
    }

    public void setCurrentOntologyId(Long ontologyId) {
        log.info("setting current ontology to {}", ontologyId);
        if (ontologyId == null) {
            SessionMgr.getSessionMgr().setModelProperty("lastSelectedOntology", null);
        }
        else {
            SessionMgr.getSessionMgr().setModelProperty("lastSelectedOntology", ontologyId.toString());
        }
        notifyOntologySelected(ontologyId);
    }

    public TreeSet<String> getOntologyTermSet(Entity ontologyRoot) {
        TreeSet<String> ontologyElementTreeSet = new TreeSet<>();
        List<Entity> list = ModelMgrUtils.getAccessibleChildren(ontologyRoot);
        list = ontologyWalker(list);
        for (Entity entity : list) {
            ontologyElementTreeSet.add(entity.getName());
        }
        return ontologyElementTreeSet;
    }

    private List<Entity> ontologyWalker(List<Entity> list) {
        List<Entity> finalList = new ArrayList<>();
        finalList.addAll(list);
        for (Entity entity : list) {
            List<Entity> accessible = ModelMgrUtils.getAccessibleChildren(entity);
            if (null != accessible) {
                finalList.addAll(ontologyWalker(accessible));
            }
        }
        return finalList;
    }

    public String getSortCriteria(Long entityId) {
        // This is waaaay to slow when loading a large entity tree. 
//        try {
//            Subject subject = getSubjectWithPreferences(SessionMgr.getSubjectKey());
//            Map<String, SubjectPreference> prefs = subject.getCategoryPreferences(CATEGORY_SORT_CRITERIA);
//            String entityIdStr = entityId.toString();
//            for (SubjectPreference pref : prefs.values()) {
//                if (pref.getName().equals(entityIdStr)) {
//                    return pref.getValue();
//                }
//            }
//        }
//        catch (Exception e) {
//            log.error("Error loading sort criteria for {}", entityId,e);
//        }
        return null;
    }

    public void saveSortCriteria(Long entityId, String sortCriteria) throws Exception {
        Subject subject = getSubjectWithPreferences(SessionMgr.getSubjectKey());
        if (StringUtils.isEmpty(sortCriteria)) {
            subject.getPreferenceMap().remove(CATEGORY_SORT_CRITERIA + ":" + entityId);
            log.debug("Removed user preference: " + CATEGORY_SORT_CRITERIA + ":" + entityId);
        }
        else {
            subject.setPreference(new SubjectPreference(entityId.toString(), CATEGORY_SORT_CRITERIA, sortCriteria));
            log.debug("Saved user preference: " + CATEGORY_SORT_CRITERIA + ":" + entityId + "=" + sortCriteria);
        }
        Subject newSubject = ModelMgr.getModelMgr().saveOrUpdateSubject(subject);
       // SessionMgr.getSessionMgr().setSubject(newSubject);
    }

    private void notifyOntologySelected(final Long ontologyId) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    for (ModelMgrObserver listener : getModelMgrObservers()) {
                        listener.ontologySelected(ontologyId);
                    }
                }
            });
        }
        else {
            for (ModelMgrObserver listener : getModelMgrObservers()) {
                listener.ontologySelected(ontologyId);
            }
        }
    }

    public void notifyOntologyChanged(final Long entityId) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    for (ModelMgrObserver listener : getModelMgrObservers()) {
                        listener.ontologyChanged(entityId);
                    }
                }
            });
        }
        else {
            for (ModelMgrObserver listener : getModelMgrObservers()) {
                listener.ontologyChanged(entityId);
            }
        }
    }

    void notifyEntitySelected(final String category, final String identifier, final boolean clearAll) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    for (ModelMgrObserver listener : getModelMgrObservers()) {
                        listener.entitySelected(category, identifier, clearAll);
                    }
                }
            });
        }
        else {
            for (ModelMgrObserver listener : getModelMgrObservers()) {
                listener.entitySelected(category, identifier, clearAll);
            }
        }
    }

    void notifyEntityDeselected(final String category, final String identifier) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    for (ModelMgrObserver listener : getModelMgrObservers()) {
                        listener.entityDeselected(category, identifier);
                    }
                }
            });
        }
        else {
            for (ModelMgrObserver listener : getModelMgrObservers()) {
                listener.entityDeselected(category, identifier);
            }
        }
    }

    public void notifyEntityChanged(final Long entityId) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    for (ModelMgrObserver listener : getModelMgrObservers()) {
                        listener.entityChanged(entityId);
                    }
                }
            });
        }
        else {
            for (ModelMgrObserver listener : getModelMgrObservers()) {
                listener.entityChanged(entityId);
            }
        }
    }

    public void notifyEntityChildrenChanged(final Long entityId) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    for (ModelMgrObserver listener : getModelMgrObservers()) {
                        listener.entityChildrenChanged(entityId);
                    }
                }
            });
        }
        else {
            for (ModelMgrObserver listener : getModelMgrObservers()) {
                listener.entityChildrenChanged(entityId);
            }
        }
    }

    public void notifyEntityRemoved(final Long entityId) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    for (ModelMgrObserver listener : getModelMgrObservers()) {
                        listener.entityRemoved(entityId);
                    }
                }
            });
        }
        else {
            for (ModelMgrObserver listener : getModelMgrObservers()) {
                listener.entityRemoved(entityId);
            }
        }
    }

    public void notifyEntityDataRemoved(final Long entityDataId) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    for (ModelMgrObserver listener : getModelMgrObservers()) {
                        listener.entityDataRemoved(entityDataId);
                    }
                }
            });
        }
        else {
            for (ModelMgrObserver listener : getModelMgrObservers()) {
                listener.entityDataRemoved(entityDataId);
            }
        }
    }

    public void notifyAnnotationsChanged(final Long entityId) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    for (ModelMgrObserver listener : getModelMgrObservers()) {
                        listener.annotationsChanged(entityId);
                    }
                }
            });
        }
        else {
            for (ModelMgrObserver listener : getModelMgrObservers()) {
                listener.annotationsChanged(entityId);
            }
        }
    }

    private void notifyAnnotationSessionSelected(final Long sessionId) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    for (ModelMgrObserver listener : getModelMgrObservers()) {
                        listener.sessionSelected(sessionId);
                    }
                }
            });
        }
        else {

            for (ModelMgrObserver listener : getModelMgrObservers()) {
                listener.sessionSelected(sessionId);
            }
        }
    }

    private void notifyAnnotationSessionDeselected() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    for (ModelMgrObserver listener : getModelMgrObservers()) {
                        listener.sessionDeselected();
                    }
                }
            });
        }
        else {
            for (ModelMgrObserver listener : getModelMgrObservers()) {
                listener.sessionDeselected();
            }
        }
    }

    public void prepareForSystemExit() {
        FacadeManager.getFacadeManager().prepareForSystemExit();
    }

    public EntitySelectionModel getEntitySelectionModel() {
        return entitySelectionModel;
    }

    public List<EntityType> getEntityTypes() throws Exception {
        return FacadeManager.getFacadeManager().getEntityFacade().getEntityTypes();
    }

    public List<EntityAttribute> getEntityAttributes() throws Exception {
        return FacadeManager.getFacadeManager().getEntityFacade().getEntityAttributes();
    }

    public Entity getEntityById(String entityId) throws Exception {
        return getEntityById(new Long(entityId));
    }

    public Entity getEntityById(Long entityId) throws Exception {
        return entityModel.getEntityById(entityId);
    }

    public List<Entity> getEntitiesByIds(List<Long> entityIds) throws Exception {
        return entityModel.getEntitiesById(entityIds);
    }

    public List<Entity> getEntitiesByName(String entityName) throws Exception {
        return entityModel.getEntitiesByName(entityName);
    }

    public List<Entity> getOwnedEntitiesByName(String entityName) throws Exception {
        return entityModel.getOwnedEntitiesByName(entityName);
    }

    public List<List<EntityData>> getPathsToRoots(Long entityId) throws Exception {
        return FacadeManager.getFacadeManager().getEntityFacade().getPathsToRoots(entityId);
    }

    public List<Entity> getParentEntities(Long childEntityId) throws Exception {
        return entityModel.getParentEntities(childEntityId);
    }

    public List<EntityData> getParentEntityDatas(Long childEntityId) throws Exception {
        return entityModel.getParentEntityDatas(childEntityId);
    }

    public List<EntityData> getAllParentEntityDatas(Long childEntityId) throws Exception {
        return entityModel.getAllParentEntityDatas(childEntityId);
    }

    public Set<Long> getParentIdsForAttribute(long childEntityId, String attributeName) throws Exception {
        return FacadeManager.getFacadeManager().getEntityFacade().getParentIdsForAttribute(childEntityId, attributeName);
    }

    public List<Entity> getEntitiesByTypeName(String entityTypeName) throws Exception {
        return entityModel.getEntitiesByTypeName(entityTypeName);
    }

    public List<Entity> getOwnedEntitiesByTypeName(String entityTypeName) throws Exception {
        return entityModel.getOwnedEntitiesByTypeName(entityTypeName);
    }

    public List<Entity> getDataSets() throws Exception {
        return entityModel.getDataSets();
    }

    public List<Entity> getFlyLineReleases() throws Exception {
        return entityModel.getFlyLineReleases();
    }
    
    public Entity createCommonRoot(String name) throws Exception {
        if (currWorkspaceId == null) {
            throw new IllegalStateException("Cannot create common root when workspace is not selected");
        }
        return entityModel.createCommonRootFolder(currWorkspaceId, name).getChildEntity();
    }

    public void demoteCommonRootToFolder(Entity commonRoot) throws Exception {
        entityModel.demoteCommonRootToFolder(commonRoot);
    }

    public void removeEntityData(EntityData ed) throws Exception {
        entityModel.deleteEntityData(ed);
        notifyEntityDataRemoved(ed.getId());
    }

    public void deleteBulkEntityData(Entity parent, Collection<EntityData> toDelete) throws Exception {
        entityModel.deleteBulkEntityData(parent, toDelete);
        for (EntityData ed : toDelete) {
            notifyEntityDataRemoved(ed.getId());
        }
    }

    public void deleteEntityTree(Long id) throws Exception {
        entityModel.deleteEntityTree(entityModel.getEntityById(id));
        notifyEntityRemoved(id);
    }

    public void deleteEntityTree(Long id, boolean unlinkMultipleParents) throws Exception {
        FacadeManager.getFacadeManager().getEntityFacade().deleteEntityTree(id, unlinkMultipleParents);
        notifyEntityRemoved(id);
    }

    public Entity createOntologyRoot(String ontologyName) throws Exception {
        return entityModel.createOntologyRoot(ontologyName);
    }

    public Entity createOntologyTerm(Long ontologyRootId, Long parentId, String label, OntologyElementType type, Integer orderIndex) throws Exception {
        Entity term = entityModel.createOntologyTerm(parentId, label, type, orderIndex);
        // Note: here we are assuming that the affected term is in the selected ontology, which is not necessarily true,
        // but it doesn't hurt to refresh the clients even if another ontology is being changed.
        if (ontologyRootId != null) {
            notifyOntologyChanged(ontologyRootId);
        }
        return term;
    }

    public Entity createOntologyAnnotation(OntologyAnnotation annotation) throws Exception {
        Entity annotationEntity = FacadeManager.getFacadeManager().getOntologyFacade().createOntologyAnnotation(annotation);
        notifyAnnotationsChanged(annotation.getTargetEntityId());
        return annotationEntity;
    }

    public void removeAnnotation(Long annotationId) throws Exception {
        Entity annotationEntity = FacadeManager.getFacadeManager().getEntityFacade().getEntityById(annotationId);
        if (annotationEntity == null || annotationEntity.getId() == null) {
            return;
        }
        OntologyAnnotation annotation = new OntologyAnnotation();
        annotation.init(annotationEntity);
        FacadeManager.getFacadeManager().getAnnotationFacade().removeAnnotation(annotationEntity.getId());
        notifyAnnotationsChanged(annotation.getTargetEntityId());
    }

    public void acceptComputationAnnotation(Long entityId, List<OntologyAnnotation> entityAnnotations) throws Exception {
        for(OntologyAnnotation annotation : entityAnnotations) {
        	if (annotation.isComputation()) {
                Entity annotationEntity = annotation.getEntity();
                if (annotationEntity == null || annotationEntity.getId() == null) {
                    return;
                }
                EntityData computationalEd = annotationEntity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_IS_COMPUTATIONAL);
                entityModel.deleteEntityData(computationalEd);
            }
        }
        notifyAnnotationsChanged(entityId);
    }
    
    public Entity getOntologyTree(Long rootId) throws Exception {
        return FacadeManager.getFacadeManager().getOntologyFacade().getOntologyTree(rootId);
    }

    public AnnotationSession getAnnotationSession(Long sessionId) throws Exception {
        if (annotationSession != null && annotationSession.getId().equals(sessionId)) {
            return annotationSession;
        }

        Task task = getTaskById(sessionId);
        if (task == null) {
            return null;
        }
        if (task instanceof AnnotationSessionTask) {
            AnnotationSessionTask ast = (AnnotationSessionTask) task;
            return new AnnotationSession(ast);
        }

        return null;
    }

    public void reset() {
        entityModel.invalidateAll();
        this.currWorkspaceId = null;
        this.annotationSession = null;
    }
    
    public void invalidateCache() {
        entityModel.invalidateAll();
        this.currWorkspaceId = null;
    }

    public void invalidateCache(Entity entity, boolean recurse) {
        if (entity.getId().equals(currWorkspaceId)) {
            this.currWorkspaceId = null;
        }
        entityModel.invalidate(entity, recurse);
    }

    public List<Entity> getWorkspaces() throws Exception {
        return entityModel.getWorkspaces();
    }

    public void setCurrentWorkspaceId(Long workspaceId) {
        this.currWorkspaceId = workspaceId;
    }

    public Entity getCurrentWorkspace() throws Exception {
        if (currWorkspaceId==null) {
            // No current workspace defined, so make it the default workspace
            for(Entity workspace : getWorkspaces()) {
                if (workspace.getName().equals(EntityConstants.NAME_DEFAULT_WORKSPACE)) {
                    setCurrentWorkspaceId(workspace.getId());    
                }
            }
        }
        Entity curr = entityModel.getEntityById(currWorkspaceId);
        log.debug("Got current workspace {} as {}",currWorkspaceId,curr);
        return curr;
    }

    public RootedEntity getOwnedTopLevelRootedEntity(String name) throws Exception {
        if (currWorkspaceId==null) return null;
        RootedEntity workspaceRE = new RootedEntity(getCurrentWorkspace());
        return workspaceRE.getOwnedChildByName(name);
    }
    
    public Entity getOwnedCommonRootByName(String name) throws Exception {
        if (currWorkspaceId==null) {
            log.warn("No workspace is loaded, can't retrieve common root "+name);
            return null;
        }
        return entityModel.getOwnedCommonRootByName(currWorkspaceId, name);
    }

    public Entity getEntityAndChildren(long entityId) throws Exception {
        return entityModel.getEntityAndChildren(entityId);
    }
    
    public List<byte[]> getB64DecodedEntityDataValues(Long entityId, String entityDataType) throws Exception {
        return entityModel.getB64DecodedEntityDataValues(entityId, entityDataType);
    }

    public byte[] getB64DecodedEntityDataValue(Long entityId, Long entityDataId, String entityDataType) throws Exception {
        return entityModel.getB64DecodedEntityDataValue(entityId, entityDataId, entityDataType);
    }

    public Entity getEntityTree(long entityId) throws Exception {
        return entityModel.getEntityTree(entityId);
    }

    public Entity refreshChildren(Entity entity) throws Exception {
        entityModel.refreshChildren(entity);
        return entityModel.getEntityById(entity.getId());
    }

    public Entity refreshEntity(Entity entity) throws Exception {
        return entityModel.reload(entity);
    }

    public Entity refreshEntityAndChildren(Entity entity) throws Exception {
        entityModel.refreshChildren(entity);
        return entityModel.reload(entity);
    }

    public Entity loadLazyEntity(Entity entity, boolean recurse) throws Exception {
        return entityModel.loadLazyEntity(entity, recurse);
    }

    public List<Entity> getOntologyRootEntities() throws Exception {
        return entityModel.getOntologyRoots();
    }

    public Entity getErrorOntology() {
        return ERROR_ONTOLOGY;
    }

    public Entity createEntity(String entityTypeName, String entityName) throws Exception {
        return entityModel.createEntity(entityTypeName, entityName);
    }

    public EntityData addEntityToParent(Entity parent, Entity entity) throws Exception {
        EntityData ed = entityModel.addEntityToParent(parent, entity);
        log.info("addEntityToParent created ed " + ed.getId() + " parent:" + ed.getParentEntity() + " child:" + ed.getChildEntity());
        notifyEntityChildrenChanged(parent.getId());
        return ed;
    }

    public EntityData addEntityToParent(Entity parent, Entity entity, Integer index, String attrName) throws Exception {
        EntityData ed = entityModel.addEntityToParent(parent, entity, index, attrName);
        notifyEntityChildrenChanged(parent.getId());
        return ed;
    }

    public long getNumDescendantsAnnotated(Long entityId) throws Exception {
        return FacadeManager.getFacadeManager().getAnnotationFacade().getNumDescendantsAnnotated(entityId);
    }

    public List<Entity> getAnnotationsForEntity(Long entityId) throws Exception {
        return FacadeManager.getFacadeManager().getAnnotationFacade().getAnnotationsForEntity(entityId);
    }

    public List<Entity> getAnnotationsForEntities(List<Long> entityIds) throws Exception {
        return FacadeManager.getFacadeManager().getAnnotationFacade().getAnnotationsForEntities(entityIds);
    }

    public List<Entity> getAnnotationsForChildren(Long parentId) throws Exception {
        return FacadeManager.getFacadeManager().getAnnotationFacade().getAnnotationsForChildren(parentId);
    }

    public void removeAllOntologyAnnotationsForSession(Long annotationSessionId) throws Exception {
        FacadeManager.getFacadeManager().getAnnotationFacade().removeAllOntologyAnnotationsForSession(annotationSessionId);
    }

    public void createEntityType(String typeName) throws Exception {
        FacadeManager.getFacadeManager().getEntityFacade().createEntityType(typeName);
    }

    public void createEntityAttribute(String typeName, String attrName) throws Exception {
        FacadeManager.getFacadeManager().getEntityFacade().createEntityAttribute(typeName, attrName);
    }

    public List<Entity> getEntitiesForAnnotationSession(Long annotationSessionId) throws Exception {
        return FacadeManager.getFacadeManager().getAnnotationFacade().getEntitiesForAnnotationSession(annotationSessionId);
    }

    public List<Entity> getCategoriesForAnnotationSession(Long annotationSessionId) throws Exception {
        return FacadeManager.getFacadeManager().getAnnotationFacade().getCategoriesForAnnotationSession(annotationSessionId);
    }

    public Set<Long> getCompletedEntityIds(Long annotationSessionId) throws Exception {
        return FacadeManager.getFacadeManager().getAnnotationFacade().getCompletedEntityIds(annotationSessionId);
    }

    public List<Entity> getAnnotationsForSession(Long annotationSessionId) throws Exception {
        return FacadeManager.getFacadeManager().getAnnotationFacade().getAnnotationsForSession(annotationSessionId);
    }

    public Entity getAncestorWithType(Entity entity, String type) throws Exception {
        return entityModel.getAncestorWithType(entity, type);
    }

    public Entity renameEntity(Entity entity, String newName) throws Exception {
        Entity newEntity = entityModel.renameEntity(entity, newName);
        if (newEntity != null) {
            notifyEntityChanged(newEntity.getId());
        }
        return newEntity;
    }

    public Entity setAttributeAsTag(Entity entity, String attributeName) throws Exception {
        Entity newEntity = entityModel.setAttributeAsTag(entity, attributeName);
        if (newEntity != null) {
            notifyEntityChanged(newEntity.getId());
        }
        return newEntity;
    }

    public Entity saveOrUpdateEntity(Entity entity) throws Exception {
        Entity newEntity = entityModel.saveEntity(entity);
        if (newEntity != null) {
            notifyEntityChanged(entity.getId());
        }
        return newEntity;
    }

    public EntityData saveOrUpdateEntityData(EntityData entityData) throws Exception {
        EntityData newEntityData = entityModel.saveEntityData(entityData);
        return newEntityData;
    }
    
    public Entity saveOrUpdateAnnotation(Entity annotatedEntity, Entity annotation) throws Exception {
        Entity newAnnotation = entityModel.saveEntity(annotation);
        if (newAnnotation != null) {
            notifyAnnotationsChanged(annotatedEntity.getId());
        }
        return newAnnotation;
    }

    public EntityData updateChildIndex(EntityData entityData, Integer orderIndex) throws Exception {
        EntityData ed = entityModel.updateChildIndex(entityData, orderIndex);
        notifyEntityChanged(entityData.getParentEntity().getId());
        return ed;
    }

    public Entity updateChildIndexes(Entity entity) throws Exception {
        Entity savedEntity = entityModel.updateChildIndexes(entity);
        notifyEntityChanged(entity.getId());
        return savedEntity;
    }

    public EntityData setOrUpdateValue(Entity entity, String attributeName, String value) throws Exception {
        EntityData ed = entityModel.setOrUpdateValue(entity, attributeName, value);
        notifyEntityChanged(entity.getId());
        return ed;
    }

    public Collection<EntityData> setOrUpdateValues(Collection<Entity> entities, String attributeName, String value) throws Exception {
        Collection<EntityData> eds = entityModel.setOrUpdateValues(entities, attributeName, value);
        for (Entity entity : entities) {
            notifyEntityChanged(entity.getId());
        }
        return eds;
    }

    public Task saveOrUpdateTask(Task task) throws Exception {
        return FacadeManager.getFacadeManager().getComputeFacade().saveOrUpdateTask(task);
    }

    public void stopContinuousExecution(ContinuousExecutionTask task) throws Exception {
        if (task == null) {
            throw new IllegalArgumentException("Task may not be null");
        }
        FacadeManager.getFacadeManager().getComputeFacade().stopContinuousExecution(task.getObjectId());
    }

    public Task getTaskById(Long taskId) throws Exception {
        return FacadeManager.getFacadeManager().getComputeFacade().getTaskById(taskId);
    }

    public void deleteTaskById(Long taskId) throws Exception {
        FacadeManager.getFacadeManager().getComputeFacade().deleteTaskById(taskId);
    }

    public void cancelTaskById(Long taskId) throws Exception {
        FacadeManager.getFacadeManager().getComputeFacade().cancelTaskById(taskId);
    }

    public Task submitJob(String processDefName, String displayName) throws Exception {
        HashSet<TaskParameter> taskParameters = new HashSet<>();
        return submitJob(processDefName, displayName, taskParameters);
    }

    public Task submitJob(String processDefName, String displayName, HashSet<TaskParameter> parameters) throws Exception {
        GenericTask task = new GenericTask(new HashSet<Node>(), SessionMgr.getSubjectKey(), new ArrayList<Event>(),
                parameters, processDefName, displayName);
        return submitJob(task);
    }

    private Task submitJob(GenericTask genericTask) throws Exception {
        Task task = saveOrUpdateTask(genericTask);
        submitJob(task.getTaskName(), task);
        return task;
    }

    public TaskRequest submitJob(String processDefName, Task task) throws Exception {
        FacadeManager.getFacadeManager().getComputeFacade().submitJob(processDefName, task.getObjectId());
        return new TaskRequest(new TaskFilter(task.getJobName(), task.getObjectId()));
    }

    /**
     * Like the submitJob method, but this one pushes the task to a dispatcher, for scheduled (balanced?)
     * retrieval by waiting servers.
     *
     * @param processDefName name for labeling.
     * @param task with all params
     * @return the requiest created.
     * @throws Exception
     */
    public TaskRequest dispatchJob(String processDefName, Task task) throws Exception {
        FacadeManager.getFacadeManager().getComputeFacade().dispatchJob(processDefName, task.getObjectId());
        return new TaskRequest(new TaskFilter(task.getJobName(), task.getObjectId()));
    }

    public List<Task> getUserParentTasks() throws Exception {
        return FacadeManager.getFacadeManager().getComputeFacade().getUserParentTasks();
    }

    public List<Task> getUserTasks() throws Exception {
        return FacadeManager.getFacadeManager().getComputeFacade().getUserTasks();
    }

    public List<Task> getUserTasksByType(String taskName) throws Exception {
        return FacadeManager.getFacadeManager().getComputeFacade().getUserTasksByType(taskName);
    }

    public Subject getSubject() throws Exception {
        return FacadeManager.getFacadeManager().getComputeFacade().getSubject();
    }

    public Subject getSubjectByKey(String key) throws Exception {
        Subject subject = subjectByKey.get(key);
        if (subject!=null) return subject;
        subject = FacadeManager.getFacadeManager().getComputeFacade().getSubject(key);
        subjectByKey.put(subject.getKey(), subject);
        return subject;
    }

    public Subject getSubjectWithPreferences(String nameOrKey) throws Exception {
        return FacadeManager.getFacadeManager().getComputeFacade().getSubject(nameOrKey);
    }

    public List<Subject> getSubjects() throws Exception {
        return FacadeManager.getFacadeManager().getComputeFacade().getSubjects();
    }

    public Subject saveOrUpdateSubject(Subject subject) throws Exception {
        return FacadeManager.getFacadeManager().getComputeFacade().saveOrUpdateSubject(subject);
    }

    public EntityActorPermission saveOrUpdatePermission(EntityActorPermission eap) throws Exception {
        return FacadeManager.getFacadeManager().getEntityFacade().saveOrUpdatePermission(eap);
    }

    public void removePreferenceCategory(String category) throws Exception {
        FacadeManager.getFacadeManager().getComputeFacade().removePreferenceCategory(category);
    }

    public SolrResults searchSolr(SolrQuery query) throws Exception {
        log.info("Searching SOLR: " + query.getQuery() + " start=" + query.getStart() + " rows=" + query.getRows());
        return FacadeManager.getFacadeManager().getSolrFacade().searchSolr(query);
    }

    public SolrResults searchSolr(SolrQuery query, boolean mapToEntities) throws Exception {
        log.info("Searching SOLR: " + query.getQuery() + " start=" + query.getStart() + " rows=" + query.getRows());
        return FacadeManager.getFacadeManager().getSolrFacade().searchSolr(query, mapToEntities);
    }

    public void updateIndex(DomainObject domainObj) throws Exception {
        FacadeManager.getFacadeManager().getSolrFacade().updateIndex(domainObj);
    }

    public void removeFromIndex(Long domainObjId) throws Exception {
         FacadeManager.getFacadeManager().getSolrFacade().removeFromIndex(domainObjId);
    }

    public void addAncestorToIndex(Long domainObjId, Long ancestorId) throws Exception {
        FacadeManager.getFacadeManager().getSolrFacade().addAncestorToIndex(domainObjId, ancestorId);
    }
    
    public Map<String, SageTerm> getImageVocabulary() throws Exception {
        return FacadeManager.getFacadeManager().getSolrFacade().getImageVocabulary();
    }

    public void addChildren(Long parentId, List<Long> childrenIds, String attributeName) throws Exception {
        entityModel.addChildren(parentId, childrenIds, attributeName);
        notifyEntityChildrenChanged(parentId);
    }

    public Entity createDataSet(String dataSetName) throws Exception {
        return entityModel.createDataSet(dataSetName);
    }

    public Entity createFlyLineRelease(String releaseName, Date releaseDate, Integer lagTimeMonths, List<String> dataSetList) throws Exception {
        return entityModel.createFlyLineRelease(releaseName, releaseDate, lagTimeMonths, dataSetList);
    }
    
    public Set<EntityActorPermission> getFullPermissions(Long entityId) throws Exception {
        return FacadeManager.getFacadeManager().getEntityFacade().getFullPermissions(entityId);
    }

    public EntityActorPermission grantPermissions(Long entityId, String subjectKey, String permissions, boolean recursive) throws Exception {
        EntityActorPermission eap = FacadeManager.getFacadeManager().getEntityFacade().grantPermissions(entityId, subjectKey, permissions, recursive);
        entityModel.reloadById(entityId);
        return eap;
    }

    public void revokePermissions(Long entityId, String subjectKey, boolean recursive) throws Exception {
        FacadeManager.getFacadeManager().getEntityFacade().revokePermissions(entityId, subjectKey, recursive);
        entityModel.reloadById(entityId);
    }

    public List<MappedId> getProjectedResults(List<Long> entityIds, List<String> upMapping, List<String> downMapping) throws Exception {
        return FacadeManager.getFacadeManager().getEntityFacade().getProjectedResults(entityIds, upMapping, downMapping);
    }

    public List<Long> getEntityIdsInAlignmentSpace(String opticalRes, String pixelRes, List<Long> guids) throws Exception {
        return FacadeManager.getFacadeManager().getAnnotationFacade().getEntityIdsInAlignmentSpace(opticalRes, pixelRes, guids);
    }

    public Object[] getPatternAnnotationQuantifierMapsFromSummary() throws Exception {
        return FacadeManager.getFacadeManager().getAnnotationFacade().getPatternAnnotationQuantifierMapsFromSummary();
    }

    public Object[] getMaskQuantifierMapsFromSummary(String maskFolderName) throws Exception {
        return FacadeManager.getFacadeManager().getAnnotationFacade().getMaskQuantifierMapsFromSummary(maskFolderName);
    }

    public List<DataDescriptor> patternSearchGetDataDescriptors(String type) throws Exception {
        return FacadeManager.getFacadeManager().getAnnotationFacade().patternSearchGetDataDescriptors(type);
    }

    public int patternSearchGetState() throws Exception {
        return FacadeManager.getFacadeManager().getAnnotationFacade().patternSearchGetState();
    }

    public List<String> patternSearchGetCompartmentList(String type) throws Exception {
        return FacadeManager.getFacadeManager().getAnnotationFacade().patternSearchGetCompartmentList(type);
    }

    public FilterResult patternSearchGetFilteredResults(String type, Map<String, Set<DataFilter>> filterMap) throws Exception {
        return FacadeManager.getFacadeManager().getAnnotationFacade().patternSearchGetFilteredResults(type, filterMap);
    }

    public void registerOnEventBus(Object object) {
        try {
            synchronized (ModelMgr.class) {
                modelEventBus.register(object);
            }
        }
        catch (IllegalArgumentException e) {
            log.warn("Cannot register object on event bus: " + e.getMessage());
        }
    }

    public void unregisterOnEventBus(Object object) {
        try {
            synchronized (ModelMgr.class) {
                modelEventBus.unregister(object);
            }
        }
        catch (IllegalArgumentException e) {
            log.warn("Cannot unregister object on event bus: " + e.getMessage());
        }
    }

    public void postOnEventBus(Object object) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Post on event bus from " + Thread.currentThread().getClass().getClassLoader() + "/" + Thread.currentThread().getContextClassLoader() + " in thread " + Thread.currentThread());
            }
            synchronized (ModelMgr.class) {
                modelEventBus.post(object);
            }
        }
        catch (IllegalArgumentException e) {
            log.warn("Cannot post event on event bus: " + e.getMessage());
        }
    }

    public Color getUserAnnotationColor(String username) {
        return userColorMapping.getColor(username);
    }

    public ThreadQueue getLoaderThreadQueue() {
        if (threadQueue == null) {
            threadQueue = new ThreadQueue(6, "LoaderGroup", Thread.MIN_PRIORITY, true);
        }
        return threadQueue;
    }

    public ThreadQueue getNotificationQueue() {
        if (notificationQueue == null) {
            if (isMultiThreaded()) {
                notificationQueue = new ThreadQueue(1, "NotificationThreads", Thread.MIN_PRIORITY, false);
            }
            else {
                notificationQueue = new ThreadQueue(0, "NotificationThreads", Thread.NORM_PRIORITY, false);
            }
        }
        return notificationQueue;
    }

    public boolean isMultiThreaded() {
//        String mt = modelMgrResourceBundle.getString("MultiThreadedServerCalls");
//        return mt != null && mt.equalsIgnoreCase("TRUE");
        return true;
    }

    // Methods associated with the 3D Tiled Microscope viewer
    public TmWorkspace createTiledMicroscopeWorkspace(Long parentId, Long brainSampleId, String name, String ownerKey) throws Exception {
        try {
            return FacadeManager.getFacadeManager().getEntityFacade().createTiledMicroscopeWorkspace(parentId, brainSampleId, name, ownerKey);
        } catch (EJBLookupException ex) {
            throw ex;
        }
    }
    
    public TmSample createTiledMicroscopeSample(String user, String sampleName, String pathToRenderFolder) throws Exception {
        TmSample sample = FacadeManager.getFacadeManager().getEntityFacade().createTiledMicroscopeSample(user, sampleName, pathToRenderFolder);
        notifyEntityChanged(sample.getId());
        return sample;
    }

    public void removeWorkspacePreference(Long workspaceId, String key) throws Exception {
        FacadeManager.getFacadeManager().getEntityFacade().removeWorkspacePreference(workspaceId, key);
    }

    public void createOrUpdateWorkspacePreference(Long workspaceId, String key, String value) throws Exception {
        FacadeManager.getFacadeManager().getEntityFacade().createOrUpdateWorkspacePreference(workspaceId, key, value);
    }

    public TmWorkspace loadWorkspace(Long workspaceId) throws Exception {
        return FacadeManager.getFacadeManager().getEntityFacade().loadWorkspace(workspaceId);
    }

    public CoordinateToRawTransform getCoordToRawTransform(String basePath) throws Exception {
        return FacadeManager.getFacadeManager().getEntityFacade().getLvvCoordToRawTransform(basePath);
    }

    public Map<Integer,byte[]> getTextureBytes( String basePath, int[] viewerCoord, int[] dimensions ) throws Exception {
        return FacadeManager.getFacadeManager().getEntityFacade().getTextureBytes(basePath, viewerCoord, dimensions);
    }
    
    public RawFileInfo getNearestChannelFiles( String basePath, int[] viewerCoord ) throws Exception {
        return FacadeManager.getFacadeManager().getEntityFacade().getNearestChannelFiles( basePath, viewerCoord );
    }

	public List<List<Object>> getRootPaths(Entity entity, Set<Long> visited) throws Exception {

        List<List<Object>> rootPaths = new ArrayList<>();
        if (!EntityConstants.TYPE_WORKSPACE.equals(entity.getEntityTypeName()) && visited.contains(entity.getId())) {
            return rootPaths;
        }
        visited.add(entity.getId());

        List<EntityData> parents = ModelMgr.getModelMgr().getParentEntityDatas(entity.getId());

        if (parents.isEmpty()) {
            List<Object> path = new ArrayList<>();
            path.add(entity);
            rootPaths.add(path);
            return rootPaths;
        }

        for (EntityData parentEd : parents) {
            Entity parent = parentEd.getParentEntity();

            if (EntityUtils.isHidden(parentEd)) {
                // Skip this path, because it should be hidden from the user
                log.debug("Skipping path becuase it's hidden: " + parent.getName() + "-(" + parentEd.getEntityAttrName() + ")->" + entity.getName());
                continue;
            }

            parent = ModelMgr.getModelMgr().getEntityById(parent.getId());
            List<List<Object>> parentPaths = getRootPaths(parent, visited);
            for (List<Object> parentPath : parentPaths) {
                parentPath.add(parentEd);
                parentPath.add(entity);
                rootPaths.add(parentPath);
            }
        }

        return rootPaths;
    }
}
