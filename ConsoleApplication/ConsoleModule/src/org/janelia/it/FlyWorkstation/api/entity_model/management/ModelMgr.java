package org.janelia.it.FlyWorkstation.api.entity_model.management;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import org.apache.solr.client.solrj.SolrQuery;
import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrObserver;
import org.janelia.it.FlyWorkstation.api.entity_model.fundtype.TaskFilter;
import org.janelia.it.FlyWorkstation.api.entity_model.fundtype.TaskRequest;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.FlyWorkstation.api.facade.roles.ExceptionHandler;
import org.janelia.it.FlyWorkstation.api.stub.data.NoDataException;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.model.domain.EntityWrapper;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.model.utils.AnnotationSession;
import org.janelia.it.FlyWorkstation.model.utils.OntologyKeyBind;
import org.janelia.it.FlyWorkstation.model.utils.OntologyKeyBindings;
import org.janelia.it.FlyWorkstation.model.viewer.AlignedItem;
import org.janelia.it.FlyWorkstation.shared.exception_handlers.PrintStackTraceHandler;
import org.janelia.it.FlyWorkstation.shared.util.ThreadQueue;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.compute.api.support.MappedId;
import org.janelia.it.jacs.compute.api.support.SageTerm;
import org.janelia.it.jacs.compute.api.support.SolrResults;
import org.janelia.it.jacs.model.entity.*;
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
import org.janelia.it.jacs.model.user_data.prefs.SubjectPreference;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;
import org.janelia.it.jacs.shared.annotation.DataDescriptor;
import org.janelia.it.jacs.shared.annotation.DataFilter;
import org.janelia.it.jacs.shared.annotation.FilterResult;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executor;

public class ModelMgr {
    
    private static final Logger log = LoggerFactory.getLogger(ModelMgr.class);
    
    // TODO: externalize these properties
    public static final String NEURON_ANNOTATOR_CLIENT_NAME = "NeuronAnnotator";
    public static final String CATEGORY_KEYBINDS_GENERAL = "Keybind:General";
    public static final String CATEGORY_KEYBINDS_ONTOLOGY = "Keybind:Ontology:";
    
    private static ModelMgr modelManager = new ModelMgr();
    private ThreadQueue threadQueue;
    private ThreadQueue notificationQueue;

    private final EventBus modelEventBus;
    private final EntityModel entityModel;
    private final EntitySelectionModel entitySelectionModel;
    private final UserColorMapping userColorMapping;

//    private Entity selectedOntology;
//    private OntologyKeyBindings ontologyKeyBindings;
    private AnnotationSession annotationSession;
    private OntologyAnnotation currentSelectedOntologyAnnotation;
    
    public Entity ERROR_ONTOLOGY = null;
    
    private final List<ModelMgrObserver> modelMgrObservers = new ArrayList<ModelMgrObserver>();
    
    
    static {
        // Register an exception handler.
        ModelMgr.getModelMgr().registerExceptionHandler(new PrintStackTraceHandler());
    }

    private ModelMgr() {        
        log.info("Initializing Model Manager");
        // Sync block may/may not be necessary. Problem may just be intermittent.
        //   Saw NPE on use of modelEventBus, during attempt to register against it.
        synchronized (ModelMgr.class) {
            log.info("ModelMgr c'tor from  " + Thread.currentThread().getClass().getClassLoader() + "/" + Thread.currentThread().getContextClassLoader() +  " in thread " + Thread.currentThread());

            this.entityModel = new EntityModel();
            this.entitySelectionModel = new EntitySelectionModel();
            this.userColorMapping = new UserColorMapping();
            this.modelEventBus = new AsyncEventBus("awt", new Executor() {
                public void execute(Runnable cmd) {
                    if (EventQueue.isDispatchThread()) {
                        cmd.run();
                    } else {
                    // TODO: this should queue the command on a queue that is aware of entity invalidation, 
                        // and does not generate other events for an entity if an invalidation is coming. 
                        // This will elimiante the "Instance mismatch" issues that we sometimes have.
                        EventQueue.invokeLater(cmd);
                    }
                }
            });

        }
        log.info("Successfully initialized");
    } //Singleton enforcement

    public static synchronized ModelMgr getModelMgr() {
        return modelManager;
    }

    public void addModelMgrObserver(ModelMgrObserver mml) {
        if (null!=mml) {modelMgrObservers.add(mml);}
    }

    public void removeModelMgrObserver(ModelMgrObserver mml) {
        if (null!=mml && modelMgrObservers.contains(mml)) {modelMgrObservers.remove(mml);}
    }

    public List<ModelMgrObserver> getModelMgrObservers() {
        return new ArrayList<ModelMgrObserver>(modelMgrObservers);
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
        if (throwable instanceof NoDataException) return;
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
            if (annotationSession == null)
                notifyAnnotationSessionDeselected();
            else
                notifyAnnotationSessionSelected(annotationSession.getId());
        }
    }

    public OntologyAnnotation getCurrentSelectedOntologyAnnotation() {
        return currentSelectedOntologyAnnotation;
    }

    public void setCurrentSelectedOntologyAnnotation(OntologyAnnotation currentSelectedOntologyAnnotation) {
        this.currentSelectedOntologyAnnotation = currentSelectedOntologyAnnotation;
    }

    public Long getCurrentOntologyId() {
        String lastSelectedOntology = (String)SessionMgr.getSessionMgr().getModelProperty("lastSelectedOntology");
        if (StringUtils.isEmpty(lastSelectedOntology)) return null;
        log.debug("Current ontology is {}", lastSelectedOntology);
        return Long.parseLong(lastSelectedOntology);
    }

    public Entity getCurrentOntology() {
        return SessionMgr.getBrowser().getOntologyOutline().getCurrentOntology();
    }
    
    public void setCurrentOntologyId(Long ontologyId) {
        log.info("setting current ontology to {}", ontologyId);
        if (ontologyId == null){
            SessionMgr.getSessionMgr().setModelProperty("lastSelectedOntology", null);
        }
        else {
            SessionMgr.getSessionMgr().setModelProperty("lastSelectedOntology", ontologyId.toString());
        }
        notifyOntologySelected(ontologyId);
    }

    public TreeSet<String> getOntologyTermSet(Entity ontologyRoot){
        TreeSet<String> ontologyElementTreeSet = new TreeSet<String>();
        List<Entity> list = ontologyRoot.getOrderedChildren();
        list = ontologyWalker(list);
        for(Entity entity : list){
            ontologyElementTreeSet.add(entity.getName());
        }
        return ontologyElementTreeSet;
    }

    public List<Entity> ontologyWalker(List<Entity> list){
        List<Entity> finalList = new ArrayList<Entity>();
        finalList.addAll(list);
        for(Entity entity:list){
            if(null!=entity.getChildren()){
                finalList.addAll(ontologyWalker(entity.getOrderedChildren()));
            }
        }
        return finalList;
    }

    public OntologyKeyBindings loadOntologyKeyBindings(long ontologyId) throws Exception {
        String category = CATEGORY_KEYBINDS_ONTOLOGY + ontologyId;
        Subject subject = ModelMgr.getModelMgr().getSubject(SessionMgr.getSessionMgr().getSubject().getKey());
        Map<String, SubjectPreference> prefs = subject.getCategoryPreferences(category);

        OntologyKeyBindings ontologyKeyBindings = new OntologyKeyBindings(subject.getKey(), ontologyId);
        for (SubjectPreference pref : prefs.values()) {
            ontologyKeyBindings.addBinding(pref.getName(), Long.parseLong(pref.getValue()));
        }
        
        return ontologyKeyBindings;
    }
    
    public void saveOntologyKeyBindings(OntologyKeyBindings ontologyKeyBindings) throws Exception {

        String category = CATEGORY_KEYBINDS_ONTOLOGY + ontologyKeyBindings.getOntologyId();
        Subject subject = ModelMgr.getModelMgr().getSubject(SessionMgr.getSessionMgr().getSubject().getKey());

        // First delete all keybinds for this ontology
        for (String key : subject.getCategoryPreferences(category).keySet()) {
            subject.getPreferenceMap().remove(category + ":" + key);
        }

        // Now re-add all the current key bindings
        Set<OntologyKeyBind> keybinds = ontologyKeyBindings.getKeybinds();
        for (OntologyKeyBind bind : keybinds) {
            subject.setPreference(new SubjectPreference(bind.getKey(), category, bind.getOntologyTermId().toString()));
        }
        
        Subject newSubject = ModelMgr.getModelMgr().saveOrUpdateSubject(subject);
        SessionMgr.getSessionMgr().setSubject(newSubject);
        notifyOntologyChanged(ontologyKeyBindings.getOntologyId());
    }

    public void removeOntologyKeyBindings(long ontologyId) throws Exception {
        ModelMgr.getModelMgr().removePreferenceCategory(CATEGORY_KEYBINDS_ONTOLOGY + ontologyId);
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
    
    public boolean notifyEntityViewRequestedInNeuronAnnotator(Long entityId) {
        if (SessionMgr.getSessionMgr().getExternalClientsByName(NEURON_ANNOTATOR_CLIENT_NAME).isEmpty()) {
            return false;
        }
        for (ModelMgrObserver listener : getModelMgrObservers()) {
            listener.entityViewRequested(entityId);
        }
        return true;
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
    
    public List<Entity> getEntityByIds(List<Long> entityIds) throws Exception {
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

    public Entity createCommonRoot(String name) throws Exception {
        return entityModel.createCommonRootFolder(name);
    }

    public RootedEntity createAlignmentBoard(String alignmentBoardName, String alignmentSpace, String opticalRes, String pixelRes) throws Exception {
        return entityModel.createAlignmentBoard(alignmentBoardName, alignmentSpace, opticalRes, pixelRes);
    }

    public AlignedItem addAlignedItem(AlignedItem parent, EntityWrapper wrapper, boolean visible) throws Exception {
        Entity parentEntity = parent.getInternalEntity();
        RootedEntity rootedEntity = entityModel.addAlignedItem(parentEntity, wrapper.getInternalEntity(), wrapper.getName(), visible);
        AlignedItem newItem = new AlignedItem(rootedEntity);
        newItem.setParent(newItem);
        parent.addChild(newItem);
        return newItem;
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
        for(EntityData ed : toDelete) {
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
        if (ontologyRootId!=null) notifyOntologyChanged(ontologyRootId);
        return term;
    }

    public Entity createOntologyAnnotation(OntologyAnnotation annotation) throws Exception {
        Entity annotationEntity = FacadeManager.getFacadeManager().getOntologyFacade().createOntologyAnnotation(annotation);
        notifyAnnotationsChanged(annotation.getTargetEntityId());
        return annotationEntity;
    }

    public void removeAnnotation(Long annotationId) throws Exception {
        Entity annotationEntity = FacadeManager.getFacadeManager().getEntityFacade().getEntityById(annotationId);
        if (annotationEntity==null || annotationEntity.getId()==null) return;
        OntologyAnnotation annotation = new OntologyAnnotation();
        annotation.init(annotationEntity);
        FacadeManager.getFacadeManager().getAnnotationFacade().removeAnnotation(annotationEntity.getId());
        notifyAnnotationsChanged(annotation.getTargetEntityId());
    }
    
    public Entity getOntologyTree(Long rootId) throws Exception {
        return FacadeManager.getFacadeManager().getOntologyFacade().getOntologyTree(rootId);
    }
    
    public AnnotationSession getAnnotationSession(Long sessionId) throws Exception {
        if (annotationSession != null && annotationSession.getId().equals(sessionId)) {
            return annotationSession;
        }
        
        Task task = getTaskById(sessionId);
        if (task == null) return null;
        if (task instanceof AnnotationSessionTask) {
            AnnotationSessionTask ast = (AnnotationSessionTask)task;
            return new AnnotationSession(ast);
        }
        
        return null;
    }
    
    public void invalidateCache() {
        entityModel.invalidateAll();
    }

    public void invalidateCache(Collection<Entity> entities, boolean recurse) {
        entityModel.invalidate(entities, recurse);
    }
    
    public void invalidate(Collection<Long> entityIds) {
        entityModel.invalidate(entityIds);
    }
    
    public void invalidateCache(Entity entity, boolean recurse) {
        entityModel.invalidate(entity, recurse);
    }
    
    public List<Entity> getCommonRootEntities() throws Exception {
        return entityModel.getCommonRoots();
    }

    public Entity getCommonRootEntityByName(String name) throws Exception {
        return entityModel.getCommonRootFolder(name);
    }
    
    public Entity getEntityAndChildren(long entityId) throws Exception {        
        return entityModel.getEntityAndChildren(entityId);
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
        log.info("addEntityToParent created ed "+ed.getId()+" parent:"+ed.getParentEntity()+" child:"+ed.getChildEntity());
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
        if (newEntity!=null) notifyEntityChanged(newEntity.getId());
        return newEntity;
    }

    public Entity setAttributeAsTag(Entity entity, String attributeName) throws Exception {
        Entity newEntity = entityModel.setAttributeAsTag(entity, attributeName);
        if (newEntity!=null) notifyEntityChanged(newEntity.getId());
        return newEntity;
    }

    public Entity saveOrUpdateEntity(Entity entity) throws Exception {
        Entity newEntity = entityModel.saveEntity(entity);
        if(newEntity!=null) notifyEntityChanged(entity.getId());
        return newEntity;
    }
    
    public Entity saveOrUpdateAnnotation(Entity annotatedEntity, Entity annotation) throws Exception {
        Entity newAnnotation = entityModel.saveEntity(annotation);
        if(newAnnotation!=null) notifyAnnotationsChanged(annotatedEntity.getId());
        return newAnnotation;
    }

    public EntityData updateChildIndex(EntityData entityData, Integer orderIndex) throws Exception {
        EntityData ed =  entityModel.updateChildIndex(entityData, orderIndex);
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
    
    public Task saveOrUpdateTask(Task task) throws Exception {
        return FacadeManager.getFacadeManager().getComputeFacade().saveOrUpdateTask(task);
    }

    public void stopContinuousExecution(ContinuousExecutionTask task) throws Exception {
        if (task == null) throw new IllegalArgumentException("Task may not be null");
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
        HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
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

    public Subject getSubject(String nameOrKey) throws Exception{
        return FacadeManager.getFacadeManager().getComputeFacade().getSubject(nameOrKey);
    }

    public List<Subject> getSubjects() throws Exception{
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
        log.info("Searching SOLR: "+query.getQuery()+" start="+query.getStart()+" rows="+query.getRows());
        return FacadeManager.getFacadeManager().getSolrFacade().searchSolr(query);
    }
    
    //todo "Flylight"? Maybe we can refctor out this explicit project knowledge?  Is there a nice, clean abstraction for this?
    public Map<String, SageTerm> getFlyLightVocabulary() throws Exception {
        return FacadeManager.getFacadeManager().getSolrFacade().getFlyLightVocabulary();
    }

    public void addChildren(Long parentId, List<Long> childrenIds, String attributeName) throws Exception {
        entityModel.addChildren(parentId, childrenIds, attributeName);
        notifyEntityChildrenChanged(parentId);
    }
    
    public Entity createDataSet(String dataSetName) throws Exception {
        return entityModel.createDataSet(dataSetName);
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
        return FacadeManager.getFacadeManager().getAnnotationFacade().getEntityIdsInAlignmentSpace( opticalRes, pixelRes, guids);
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
            synchronized( ModelMgr.class ) {
                modelEventBus.register(object);
            }
        }
        catch (IllegalArgumentException e) {
            log.warn("Cannot register object on event bus: "+e.getMessage());
        }
    }
    
    public void unregisterOnEventBus(Object object) {
        try {
            synchronized( ModelMgr.class ) {
                modelEventBus.unregister(object);
            }
        }
        catch (IllegalArgumentException e) {
            log.warn("Cannot unregister object on event bus: "+e.getMessage());
        }
    }

    public void postOnEventBus(Object object) {
        try {
            log.info("Post on event bus from " + Thread.currentThread().getClass().getClassLoader() + "/"+ Thread.currentThread().getContextClassLoader()+ " in thread " + Thread.currentThread());
            synchronized( ModelMgr.class ) {
                modelEventBus.post(object);
            }
        }
        catch (IllegalArgumentException e) {
            log.warn("Cannot post event on event bus: "+e.getMessage());
        }
    }
    
    public Color getUserAnnotationColor(String username) {
        return userColorMapping.getColor(username);
    }

    public ThreadQueue getLoaderThreadQueue() {
        if (threadQueue==null){
            threadQueue=new ThreadQueue(6,"LoaderGroup",Thread.MIN_PRIORITY,true);
        }
        return threadQueue;
    }

    public ThreadQueue getNotificationQueue() {
        if (notificationQueue==null)
            if (isMultiThreaded())  notificationQueue=new ThreadQueue(1,"NotificationThreads",Thread.MIN_PRIORITY,false);
            else notificationQueue=new ThreadQueue(0,"NotificationThreads",Thread.NORM_PRIORITY,false);
        return notificationQueue;
    }

    public boolean isMultiThreaded() {
//        String mt = modelMgrResourceBundle.getString("MultiThreadedServerCalls");
//        return mt != null && mt.equalsIgnoreCase("TRUE");
        return true;
    }


    // Methods associated with the 3D Tiled Microscope viewer
    public TmWorkspace createTiledMicroscopeWorkspace(Long parentId, Long brainSampleId, String name, String ownerKey) throws Exception {
        return FacadeManager.getFacadeManager().getEntityFacade().createTiledMicroscopeWorkspace(parentId, brainSampleId, name, ownerKey);
    }

    public TmNeuron createTiledMicroscopeNeuron(Long workspaceId, String name) throws Exception {
        return FacadeManager.getFacadeManager().getEntityFacade().createTiledMicroscopeNeuron(workspaceId, name);
    }

    public TmSample createTiledMicroscopeSample(String user, String sampleName, String pathToRenderFolder) throws Exception {
        TmSample sample = FacadeManager.getFacadeManager().getEntityFacade().createTiledMicroscopeSample(user, sampleName, pathToRenderFolder);
        notifyEntityChanged(sample.getId());
        return sample;
    }

    public TmGeoAnnotation addGeometricAnnotation(Long neuronId, Long parentAnnotationId, int index,
                                                  double x, double y, double z, String comment) throws Exception {
        return FacadeManager.getFacadeManager().getEntityFacade().addGeometricAnnotation(neuronId, parentAnnotationId, index, x, y, z, comment);
    }

    public void reparentGeometricAnnotation(TmGeoAnnotation annotation, Long newParentAnnotationID,
                                            TmNeuron neuron) throws Exception {
        FacadeManager.getFacadeManager().getEntityFacade().reparentGeometricAnnotation(annotation, newParentAnnotationID, neuron);
    }

    public void rerootNeurite(TmNeuron neuron, TmGeoAnnotation newRoot) throws Exception {
        FacadeManager.getFacadeManager().getEntityFacade().rerootNeurite(neuron, newRoot);
    }

    public void updateGeometricAnnotation(TmGeoAnnotation geoAnnotation,
                                          int index, double x, double y, double z, String comment) throws Exception {
        FacadeManager.getFacadeManager().getEntityFacade().updateGeometricAnnotation(geoAnnotation, index, x, y, z, comment);
    }

    public List<TmWorkspaceDescriptor> getWorkspacesForBrainSample(Long brainSampleId, String ownerKey) throws Exception {
        return FacadeManager.getFacadeManager().getEntityFacade().getWorkspacesForBrainSample(brainSampleId, ownerKey);
    }

    public List<TmNeuronDescriptor> getNeuronsForWorkspace(Long workspaceId, String ownerKey) throws Exception {
        return FacadeManager.getFacadeManager().getEntityFacade().getNeuronsForWorkspace(workspaceId, ownerKey);
    }

    public void removeWorkspacePreference(Long workspaceId, String key) throws Exception {
        FacadeManager.getFacadeManager().getEntityFacade().removeWorkspacePreference(workspaceId, key);
    }

    public void createOrUpdateWorkspacePreference(Long workspaceId, String key, String value) throws Exception {
        FacadeManager.getFacadeManager().getEntityFacade().createOrUpdateWorkspacePreference(workspaceId, key, value);
    }

    public void deleteNeuron(String ownerKey, Long neuronId) throws Exception {
        FacadeManager.getFacadeManager().getEntityFacade().deleteNeuron(ownerKey, neuronId);
    }

    public void deleteWorkspace(String ownerKey, Long workspaceId) throws Exception {
        FacadeManager.getFacadeManager().getEntityFacade().deleteWorkspace(ownerKey, workspaceId);
    }

    public void deleteGeometricAnnotation(Long geoId) throws Exception {
        FacadeManager.getFacadeManager().getEntityFacade().deleteGeometricAnnotation(geoId);
    }

    public TmWorkspace loadWorkspace(Long workspaceId) throws Exception {
        return FacadeManager.getFacadeManager().getEntityFacade().loadWorkspace(workspaceId);
    }

    public TmNeuron loadNeuron(Long neuronId) throws Exception {
        return FacadeManager.getFacadeManager().getEntityFacade().loadNeuron(neuronId);
    }

    public TmAnchoredPath addAnchoredPath(Long neuronID, Long annotationID1, Long annotationID2,
                                          List<List<Integer>> pointlist) throws Exception {
        return FacadeManager.getFacadeManager().getEntityFacade().addAnchoredPath(neuronID, annotationID1,
                annotationID2, pointlist);
    }

    public void updateAnchoredPath(TmAnchoredPath anchoredPath, Long annotationID1, Long annotationID2,
                List<List<Integer>> pointList) throws Exception {
        FacadeManager.getFacadeManager().getEntityFacade().updateAnchoredPath(anchoredPath, annotationID1,
                annotationID2, pointList);
    }

    public void deleteAnchoredPath(Long pathID) throws Exception {
        FacadeManager.getFacadeManager().getEntityFacade().deleteAnchoredPath(pathID);
    }
}
