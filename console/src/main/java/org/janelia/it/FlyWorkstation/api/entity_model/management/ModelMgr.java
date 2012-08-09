package org.janelia.it.FlyWorkstation.api.entity_model.management;

import org.apache.solr.client.solrj.SolrQuery;
import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrObserver;
import org.janelia.it.FlyWorkstation.api.entity_model.fundtype.ActiveThreadModel;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.InUseProtocolListener;
import org.janelia.it.FlyWorkstation.api.facade.roles.ExceptionHandler;
import org.janelia.it.FlyWorkstation.api.stub.data.NoDataException;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.OntologyKeyBind;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.OntologyKeyBindings;
import org.janelia.it.FlyWorkstation.gui.framework.outline.AnnotationSession;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.exception_handlers.PrintStackTraceHandler;
import org.janelia.it.FlyWorkstation.shared.util.ThreadQueue;
import org.janelia.it.jacs.compute.api.support.MappedId;
import org.janelia.it.jacs.compute.api.support.SageTerm;
import org.janelia.it.jacs.compute.api.support.SolrResults;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.model.ontology.OntologyElement;
import org.janelia.it.jacs.model.ontology.OntologyRoot;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.annotation.AnnotationSessionTask;
import org.janelia.it.jacs.model.tasks.utility.ContinuousExecutionTask;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.prefs.UserPreference;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class ModelMgr {
	
	// TODO: externalize these properties
	private static final String NEURON_ANNOTATOR_CLIENT_NAME = "NeuronAnnotator";
	public static final String CATEGORY_KEYBINDS_GENERAL = "Keybind:General";
    public static final String CATEGORY_KEYBINDS_ONTOLOGY = "Keybind:Ontology:";
    public static OntologyRoot ERROR_ONTOLOGY_ENTITY = null;
    
    private static ModelMgr modelManager = new ModelMgr();
    private boolean readOnly;
    private final List<ModelMgrObserver> modelMgrObservers = new ArrayList<ModelMgrObserver>();
    private boolean modelAvailable;
    
    private Set<Entity> ontologies = new HashSet<Entity>();
    private ThreadQueue threadQueue;
    private ThreadQueue notificationQueue;
    private ResourceBundle modelMgrResourceBundle;
    
    private EntitySelectionModel entitySelectionModel = new EntitySelectionModel();
    private UserColorMapping userColorMapping = new UserColorMapping();
    private OntologyRoot selectedOntology;
    private OntologyKeyBindings ontologyKeyBindings;
    private AnnotationSession annotationSession;
    
    
    static {
        // Register an exception handler.
        ModelMgr.getModelMgr().registerExceptionHandler(new PrintStackTraceHandler());
    }

    {
        try {
//       String propsFile = PropertyConfigurator.getProperties().getProperty("console.ModelMgrProperties");
//       modelMgrResourceBundle = ResourceBundle.getBundle(propsFile);
        }
        catch (java.util.MissingResourceException mre) {
            System.out.println("ModelMgr: Error! - Cannot find resource files.  Resource directory must be on the classpath!!  Exiting..");
            System.exit(1);
        }
    }

    private ModelMgr() {
        FacadeManager.addInUseProtocolListener(new MyInUseProtocolListener());
    } //Singleton enforcement

    static public ModelMgr getModelMgr() {
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

    public void initErrorOntology(){
        SimpleWorker worker = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                ERROR_ONTOLOGY_ENTITY = getOntology(getErrorOntology().getId());
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

    /**
     * Override the read-only state to true for all Ontologies
     */

    public void makeReadOnly() {
        readOnly = true;
    }

    public boolean isMultiThreaded() {
        String mt = modelMgrResourceBundle.getString("MultiThreadedServerCalls");
        return mt != null && mt.equalsIgnoreCase("TRUE");
    }


    public void registerFacadeManagerForProtocol(String protocol, Class facadeClass, String displayName) {
        FacadeManager.registerFacade(protocol, facadeClass, displayName);
    }

    public ThreadQueue getLoaderThreadQueue() {
        if (threadQueue == null)
            if (isMultiThreaded()) threadQueue = new ThreadQueue(6, "LoaderGroup", Thread.MIN_PRIORITY, true);
            else threadQueue = new ThreadQueue(0, "LoaderGroup", Thread.NORM_PRIORITY, true);
        return threadQueue;
    }

    public ThreadQueue getNotificationQueue() {
        if (notificationQueue == null) if (isMultiThreaded())
            notificationQueue = new ThreadQueue(1, "NotificationThreads", Thread.MIN_PRIORITY, false);
        else notificationQueue = new ThreadQueue(0, "NotificationThreads", Thread.NORM_PRIORITY, false);
        return notificationQueue;
    }


    public ActiveThreadModel getActiveThreadModel() {
        return ActiveThreadModel.getActiveThreadModel();
    }

    public void handleException(Throwable throwable) {
        if (throwable instanceof NoDataException) return;
        FacadeManager.handleException(throwable);
    }

    public UserColorMapping getUserColorMapping() {
		return userColorMapping;
	}

	public void removeAllOntologies() {
        ontologies = null;
    }
//    public Set<Entity> getOntologies() {
//        if (ontologyLookupNeeded) {
//            OntologyFacade locator;
//            try {
//                locator = FacadeManager.getFacadeManager().getOntologyFacade();
//            }
//            catch (Exception ex) {
//                handleException(ex);
//                return new HashSet<Entity>(0);
//            }
//            List<Entity> ontologies = locator.getOntologies();
//            for (Entity ontology : ontologies) {
////             if (readOnly && !ontology.isReadOnly()) ontology.makeReadOnly();
//                this.ontologies.add(ontology);
////                if (getModelMgrObservers() != null) {
////                    Object[] listeners = getModelMgrObservers().toArray();
////                    for (Object listener : listeners) {
////                        ((ModelMgrObserver) listener).ontologyAdded(ontology);
////                    }
////                }
//            }
//            ontologyLookupNeeded = false;
//        }
//        return new HashSet<Entity>(ontologies);
//    }

    /**
     * Will NOT Force load of Ontologies
     */
    public int getNumberOfLoadedOntologies() {
        if (ontologies == null) {
            return 0;
        }
        return ontologies.size();
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

    public OntologyRoot getCurrentOntology() {
        return selectedOntology;
    }

    public void setCurrentOntology(OntologyRoot ontology) {
        if(ontology == null){
            SessionMgr.getSessionMgr().setModelProperty("lastSelectedOntology", null);
        }

        else{
            if(selectedOntology == null || !selectedOntology.getId().equals(ontology.getId()) || !selectedOntology.isFullyLoaded()) {
                SessionMgr.getSessionMgr().setModelProperty("lastSelectedOntology", ontology.getId().toString());
                modelAvailable = true;
                selectedOntology = ontology;
                notifyOntologySelected(ontology.getId());
            }
        }
    }

    public TreeSet<String> getOntologyTermSet(OntologyRoot ontologyRoot){
        TreeSet<String> ontologyElementTreeSet = new TreeSet<String>();
        List<OntologyElement> list = ontologyRoot.getChildren();
        list = ontologyWalker(list);
        for(OntologyElement element:list){
            ontologyElementTreeSet.add(element.getName());
        }
        return ontologyElementTreeSet;
    }

    public List<OntologyElement> ontologyWalker(List<OntologyElement> list){
        List<OntologyElement> finalList = new ArrayList<OntologyElement>();
        finalList.addAll(list);
        for(OntologyElement element:list){

            if(null!=element.getChildren()){
                finalList.addAll(ontologyWalker(element.getChildren()));
            }
        }
        return finalList;
    }


    private OntologyKeyBindings loadOntologyKeyBindings(long ontologyId) {
    	String category = CATEGORY_KEYBINDS_ONTOLOGY + ontologyId;
    	User user = SessionMgr.getSessionMgr().getUser();
        Map<String, UserPreference> prefs = user.getCategoryPreferences(category);

        OntologyKeyBindings ontologyKeyBindings = new OntologyKeyBindings(user.getUserLogin(), ontologyId);
        for (UserPreference pref : prefs.values()) {
            ontologyKeyBindings.addBinding(pref.getName(), Long.parseLong(pref.getValue()));
        }
        
        return ontologyKeyBindings;
    }
    
	public OntologyKeyBindings getKeyBindings(long ontologyId) {
		if (selectedOntology != null && ontologyId == selectedOntology.getId()) {
			if (ontologyKeyBindings == null || selectedOntology.getId() != ontologyKeyBindings.getOntologyId()) {
				ontologyKeyBindings = loadOntologyKeyBindings(ontologyId);
			}
			return ontologyKeyBindings;
		}
		return loadOntologyKeyBindings(ontologyId);
	}
	
    public void saveOntologyKeyBindings(OntologyKeyBindings ontologyKeyBindings) throws Exception {

        String category = CATEGORY_KEYBINDS_ONTOLOGY + ontologyKeyBindings.getOntologyId();
        User user = SessionMgr.getSessionMgr().getUser();

        // Delete all keybinds first, to maintain one key per entity
        for (String key : user.getCategoryPreferences(category).keySet()) {
            user.getPreferenceMap().remove(category + ":" + key);
        }

    	Set<OntologyKeyBind> keybinds = ontologyKeyBindings.getKeybinds();
        for (OntologyKeyBind bind : keybinds) {
            user.setPreference(new UserPreference(bind.getKey(), category, bind.getOntologyTermId().toString()));
        }

        ModelMgr.getModelMgr().saveOrUpdateUser(user);
        
        if (selectedOntology!=null) notifyOntologyChanged(selectedOntology.getId()); // See note in createOntologyTerm
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


    public boolean modelsDoUniquenessChecking() {
        String uc = modelMgrResourceBundle.getString("UniquenessCheckingOfEntities");
        return uc != null && uc.equalsIgnoreCase("TRUE");
    }

    public boolean isModelAvailable() {
        return modelAvailable;
    }

	public EntitySelectionModel getEntitySelectionModel() {
		return entitySelectionModel;
	}
	
	public List<EntityType> getEntityTypes() {
        return FacadeManager.getFacadeManager().getEntityFacade().getEntityTypes();
    }

	public List<EntityAttribute> getEntityAttributes() {
        return FacadeManager.getFacadeManager().getEntityFacade().getEntityAttributes();
    }
	
    public Entity getEntityById(String entityId) throws Exception {
        return FacadeManager.getFacadeManager().getEntityFacade().getEntityById(entityId);
    }
    
    public List<Entity> getEntityByIds(List<Long> entityIds) throws Exception {
        return FacadeManager.getFacadeManager().getEntityFacade().getEntitiesById(entityIds);
    }

    public List<Entity> getEntitiesByName(String entityName) {
        return FacadeManager.getFacadeManager().getEntityFacade().getEntitiesByName(entityName);
    }

    public List<List<EntityData>> getPathsToRoots(Long entityId) throws Exception {
        return FacadeManager.getFacadeManager().getEntityFacade().getPathsToRoots(entityId);
    }

    public List<Entity> getParentEntities(Long childEntityId) {
        return FacadeManager.getFacadeManager().getEntityFacade().getParentEntities(childEntityId);
    }

    public List<EntityData> getParentEntityDatas(Long childEntityId) {
        return FacadeManager.getFacadeManager().getEntityFacade().getParentEntityDatas(childEntityId);
    }
    
    public Set<Long> getParentIdsForAttribute(long childEntityId, String attributeName) {
    	return FacadeManager.getFacadeManager().getEntityFacade().getParentIdsForAttribute(childEntityId, attributeName);
    }
    
    public List<Entity> getEntitiesByTypeName(String entityTypeName) {
        return FacadeManager.getFacadeManager().getEntityFacade().getEntitiesByTypeName(entityTypeName);
    }

    public boolean deleteEntityById(Long entityId) throws Exception {
        boolean success = FacadeManager.getFacadeManager().getEntityFacade().deleteEntityById(entityId);
        notifyEntityRemoved(entityId);
        return success;
    }

    public void removeEntityData(EntityData ed) throws Exception {
        FacadeManager.getFacadeManager().getEntityFacade().removeEntityData(ed);
        notifyEntityDataRemoved(ed.getId());
    }
    
    public void deleteEntityTree(Long id) {
        try {
            FacadeManager.getFacadeManager().getEntityFacade().deleteEntityTree(id);
            notifyEntityRemoved(id);
        }
        catch (Exception e) {
            handleException(e);
        }
    }
    
    public Entity createOntologyRoot(String ontologyName) throws Exception {
        return FacadeManager.getFacadeManager().getOntologyFacade().createOntologyRoot(ontologyName);
    }

    public EntityData createOntologyTerm(Long id, String label, OntologyElementType type, Integer orderIndex) throws Exception {
    	EntityData ed = FacadeManager.getFacadeManager().getOntologyFacade().createOntologyTerm(id, label, type, orderIndex);
        // Note: here we are assuming that the affected term is in the selected ontology, which is not necessarily true,
        // but it doesn't hurt to refresh the clients even if another ontology is being changed.
        if (selectedOntology!=null) notifyOntologyChanged(selectedOntology.getId());
        return ed;
    }

    public Entity createOntologyAnnotation(OntologyAnnotation annotation) throws Exception {
        Entity annotationEntity = FacadeManager.getFacadeManager().getOntologyFacade().createOntologyAnnotation(annotation);
        notifyAnnotationsChanged(annotation.getTargetEntityId());
        return annotationEntity;
    }


    public void removeAnnotation(Long annotationId) throws Exception {
    	Entity annotationEntity = FacadeManager.getFacadeManager().getEntityFacade().getEntityById(annotationId.toString());
    	if (annotationEntity==null || annotationEntity.getId()==null) return;
    	OntologyAnnotation annotation = new OntologyAnnotation();
    	annotation.init(annotationEntity);
        FacadeManager.getFacadeManager().getAnnotationFacade().removeAnnotation(annotationEntity.getId());
        notifyAnnotationsChanged(annotation.getTargetEntityId());
    }
    
    public Entity getOntologyTree(Long rootId) throws Exception {
        return FacadeManager.getFacadeManager().getOntologyFacade().getOntologyTree(rootId);
    }

    public OntologyRoot getOntology(Long rootId) {
    	if (null!= selectedOntology){
            if(selectedOntology.getId().equals(rootId)){
                return selectedOntology;
            }
        }
    	try {
    		return new OntologyRoot(getOntologyTree(rootId));
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
    	return null;
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

    public List<Entity> getUserCommonRootEntitiesByTypeName(String entityTypeName) {
        return FacadeManager.getFacadeManager().getEntityFacade().getUserCommonRootEntitiesByTypeName(entityTypeName);
    }
    
    public List<Entity> getSystemCommonRootEntitiesByTypeName(String entityTypeName) {
        return FacadeManager.getFacadeManager().getEntityFacade().getSystemCommonRootEntitiesByTypeName(entityTypeName);
    }

    public Entity getEntityTree(long entityId) throws Exception {
        return FacadeManager.getFacadeManager().getEntityFacade().getEntityTree(entityId);
    }

    public Set<Entity> getChildEntities(Long parentEntityId) {
        return FacadeManager.getFacadeManager().getEntityFacade().getChildEntities(parentEntityId);
    }

    public List<Entity> getPrivateOntologies() throws Exception {
        return FacadeManager.getFacadeManager().getOntologyFacade().getPrivateOntologies();
    }

    public List<Entity> getPublicOntologies() throws Exception {
        return FacadeManager.getFacadeManager().getOntologyFacade().getPublicOntologies();
    }

    public Entity getErrorOntology() throws Exception{
        return FacadeManager.getFacadeManager().getOntologyFacade().getErrorOntology();
    }

    public Entity publishOntology(Long ontologyEntityId, String rootName) throws Exception {
        return FacadeManager.getFacadeManager().getOntologyFacade().publishOntology(ontologyEntityId, rootName);
    }

    public void removeOntologyTerm(Long termEntityId) throws Exception {
        FacadeManager.getFacadeManager().getOntologyFacade().removeOntologyTerm(termEntityId);
        if (selectedOntology!=null) notifyOntologyChanged(selectedOntology.getId()); // See note in createOntologyTerm
    }

    public Entity cloneEntityTree(Long entityId, String rootName) throws Exception {
        return FacadeManager.getFacadeManager().getEntityFacade().cloneEntityTree(entityId, rootName);
    }

    public Entity createEntity(String entityTypeName, String entityName) throws Exception {
        return FacadeManager.getFacadeManager().getEntityFacade().createEntity(entityTypeName, entityName);
    }

    public EntityData addEntityToParent(Entity parent, Entity entity, Integer index, String attrName) throws Exception {
    	EntityData ed = FacadeManager.getFacadeManager().getEntityFacade().addEntityToParent(parent, entity, index, attrName);
    	parent.getEntityData().add(ed);
    	notifyEntityChildrenChanged(parent.getId());
    	return ed;
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
    	FacadeManager.getFacadeManager().getAnnotationFacade().createEntityType(typeName);
    }
    
    public void createEntityAttribute(String typeName, String attrName) throws Exception {
    	FacadeManager.getFacadeManager().getAnnotationFacade().createEntityAttribute(typeName, attrName);
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
        return FacadeManager.getFacadeManager().getAnnotationFacade().getAncestorWithType(entity, type);
    }

    public List<List<Long>> searchTreeForNameStartingWith(Long rootId, String searchString) throws Exception {
        return FacadeManager.getFacadeManager().getAnnotationFacade().searchTreeForNameStartingWith(rootId, searchString);
    }
    
    public Entity saveOrUpdateEntity(Entity entity) throws Exception {
        Entity newEntity = FacadeManager.getFacadeManager().getEntityFacade().saveEntity(entity);
        if (newEntity!=null) notifyEntityChanged(newEntity.getId());
        return newEntity;
    }

    public Entity saveOrUpdateAnnotation(Entity annotatedEntity, Entity annotation) throws Exception {
        Entity newAnnotation = FacadeManager.getFacadeManager().getAnnotationFacade().saveEntity(annotation);
        if(newAnnotation!=null) notifyAnnotationsChanged(annotatedEntity.getId());
        return newAnnotation;
    }
    
    public EntityData saveOrUpdateEntityData(EntityData newEntityData) throws Exception {
        return FacadeManager.getFacadeManager().getEntityFacade().saveEntityDataForEntity(newEntityData);
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

    public void submitJob(String processDefName, Long taskId) throws Exception {
        FacadeManager.getFacadeManager().getComputeFacade().submitJob(processDefName, taskId);
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

    public User getUser() throws Exception {
        return FacadeManager.getFacadeManager().getComputeFacade().getUser();
    }

    public List<User> getUsers() throws Exception{
        return FacadeManager.getFacadeManager().getComputeFacade().getUsers();
    }

    public User saveOrUpdateUser(User user) throws Exception {
        return FacadeManager.getFacadeManager().getComputeFacade().saveOrUpdateUser(user);
    }

    public void removePreferenceCategory(String category) throws Exception {
        FacadeManager.getFacadeManager().getComputeFacade().removePreferenceCategory(category);
    }

    public SolrResults searchSolr(SolrQuery query) throws Exception {
    	System.out.println("Searching SOLR: "+query.getQuery()+" start="+query.getStart()+" rows="+query.getRows());
    	return FacadeManager.getFacadeManager().getSolrFacade().searchSolr(query);
    }
    
    public Map<String, SageTerm> getFlyLightVocabulary() throws Exception {
    	return FacadeManager.getFacadeManager().getSolrFacade().getFlyLightVocabulary();
    }

    public boolean loginUser() throws Exception {
        return FacadeManager.getFacadeManager().getComputeFacade().loginUser();
    }
    
    public void addChildren(Long parentId, List<Long> childrenIds, String attributeName) throws Exception {
    	FacadeManager.getFacadeManager().getAnnotationFacade().addChildren(parentId, childrenIds, attributeName);
    	notifyEntityChildrenChanged(parentId);
    }
    
    public List<MappedId> getProjectedResults(List<Long> entityIds, List<String> upMapping, List<String> downMapping) throws Exception {
    	return FacadeManager.getFacadeManager().getAnnotationFacade().getProjectedResults(entityIds, upMapping, downMapping);
    }
    
    public List<Entity> getProjectedEntities(Long entityId, List<String> upMapping, List<String> downMapping) throws Exception {
    	List<Long> startEntityIds = new ArrayList<Long>();
    	startEntityIds.add(entityId);
    	List<MappedId> mappedIds = getProjectedResults(startEntityIds, upMapping, downMapping);
    	List<Long> entityIds = new ArrayList<Long>();
    	for(MappedId mappedId : mappedIds) {
    		entityIds.add(mappedId.getMappedId());
    	}
    	return getEntityByIds(entityIds);
    }
    
    public Object[] getPatternAnnotationQuantifierMapsFromSummary() throws Exception {
        return FacadeManager.getFacadeManager().getAnnotationFacade().getPatternAnnotationQuantifierMapsFromSummary();
    }

    public Object[] getMaskQuantifierMapsFromSummary(String maskFolderName) throws Exception {
        return FacadeManager.getFacadeManager().getAnnotationFacade().getMaskQuantifierMapsFromSummary(maskFolderName);
    }

    //  private void workSpaceWasCreated(GenomeVersion genomeVersion) {
//    Set genomeVersions=getGenomeVersions();
//    GenomeVersion gv;
//      for (Object genomeVersion1 : genomeVersions) {
//          gv = (GenomeVersion) genomeVersion1;
//          if (!genomeVersion.equals(gv)) gv.makeReadOnly();
//      }
//    FacadeManager.setGenomeVersionWithWorkSpaceId(genomeVersion.getID());
//    if (getModelMgrObservers()!=null) {
//       Object[] listeners=getModelMgrObservers().toArray();
//        for (Object listener : listeners) {
//            ((ModelMgrObserver) listener).workSpaceCreated(genomeVersion);
//        }
//    }
//  }
//
//  private void workSpaceWasRemoved(GenomeVersion genomeVersion,Workspace workspace) {
//    FacadeManager.setGenomeVersionWithWorkSpaceId(0);
//    if (getModelMgrObservers()!=null) {
//       Object[] listeners=getModelMgrObservers().toArray();
//        for (Object listener : listeners) {
//            ((ModelMgrObserver) listener).workSpaceRemoved(genomeVersion, workspace);
//        }
//    }
//  }
//
//  class MyOntologyObserver extends OntologyObserverAdapter {
//       private Set observedGenomeVersions=new HashSet();
//
//       void addGenomeVersionToObserve(GenomeVersion genomeVersion) {
//          if (observedGenomeVersions.contains(genomeVersion)) return;
//          observedGenomeVersions.add(genomeVersion);
//          genomeVersion.addGenomeVersionObserver(this);
//       }
//
//       public void noteWorkspaceCreated(GenomeVersion genomeVersion, Workspace workspace){
//          workSpaceWasCreated(genomeVersion);
//       }
//
//       public void noteWorkspaceRemoved(GenomeVersion genomeVersion, Workspace workspace){
//          workSpaceWasRemoved(genomeVersion,workspace);
//       }
//    }
//
    class MyInUseProtocolListener implements InUseProtocolListener {
        public void protocolAddedToInUseList(String protocol) {
        }

        public void protocolRemovedFromInUseList(String protocol) {
        }
    }

	public Color getUserAnnotationColor(String username) {
		return userColorMapping.getColor(username);
	}
	
	// TODO: move this to a security module
	public boolean hasAccess(Entity entity) {
		String ul = entity.getUser().getUserLogin();
		return (User.SYSTEM_USER_LOGIN.equals(ul) || SessionMgr.getUsername().equals(ul));
	}
	
	public boolean hasAccess(EntityData entityData) {
		String ul = entityData.getUser().getUserLogin();
		return (User.SYSTEM_USER_LOGIN.equals(ul) || SessionMgr.getUsername().equals(ul));
	}
}


