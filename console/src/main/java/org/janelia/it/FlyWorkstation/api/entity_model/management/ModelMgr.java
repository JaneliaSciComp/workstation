package org.janelia.it.FlyWorkstation.api.entity_model.management;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrObserver;
import org.janelia.it.FlyWorkstation.api.entity_model.fundtype.ActiveThreadModel;
import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.OntologyFacade;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.InUseProtocolListener;
import org.janelia.it.FlyWorkstation.api.facade.roles.ExceptionHandler;
import org.janelia.it.FlyWorkstation.api.stub.data.NoDataException;
import org.janelia.it.FlyWorkstation.shared.exception_handlers.PrintStackTraceHandler;
import org.janelia.it.FlyWorkstation.shared.util.ThreadQueue;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.User;

import java.lang.reflect.Constructor;
import java.util.*;

public class ModelMgr {
    private static ModelMgr modelManager = new ModelMgr();
    private boolean readOnly;
    private final List<ModelMgrObserver> modelMgrObservers = new ArrayList<ModelMgrObserver>();
    private boolean modelAvailable;
    private Set<Entity> ontologies = new HashSet<Entity>();
    private ThreadQueue threadQueue;
    private ThreadQueue notificationQueue;
    private ResourceBundle modelMgrResourceBundle;
    private EntityFactory entityFactory;
    private Entity selectedOntology;
    private Task annotationSesisonTask;
    private Class factoryClass;
    private boolean ontologyLookupNeeded = true;

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
        modelMgrObservers.add(mml);
    }

    public void removeModelMgrObserver(ModelMgrObserver mml) {
        modelMgrObservers.remove(mml);
    }


    public void registerExceptionHandler(ExceptionHandler handler) {
        FacadeManager.registerExceptionHandler(handler);
    }

    public void deregisterExceptionHandler(ExceptionHandler handler) {
        FacadeManager.deregisterExceptionHandler(handler);
    }

    /**
     * Override the read-only state to true for all Ontologies
     */

    public void makeReadOnly() {
        readOnly = true;
    }

    public EntityFactory getEntityFactory() {
        if (entityFactory == null) {
            try {
                Constructor cons = factoryClass.getConstructor(new Class[]{Integer.class});
                entityFactory = (EntityFactory) cons.newInstance(new Object[]{new Integer(this.hashCode())});
            }
            catch (Exception ex) {
                handleException(ex);
            }
        }
        return entityFactory;
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

    public void removeAllOntologies() {
        ontologies = null;
    }

    public Set<Entity> getOntologies() {
        if (ontologyLookupNeeded) {
            OntologyFacade locator;
            try {
                locator = FacadeManager.getFacadeManager().getOntologyFacade();
            }
            catch (Exception ex) {
                handleException(ex);
                return new HashSet<Entity>(0);
            }
            List<Entity> ontologies = locator.getOntologies();
            for (Entity ontology : ontologies) {
//             if (readOnly && !ontology.isReadOnly()) ontology.makeReadOnly();
                this.ontologies.add(ontology);
//                if (modelMgrObservers != null) {
//                    Object[] listeners = modelMgrObservers.toArray();
//                    for (Object listener : listeners) {
//                        ((ModelMgrObserver) listener).ontologyAdded(ontology);
//                    }
//                }
            }
            ontologyLookupNeeded = false;
        }
        return new HashSet<Entity>(ontologies);
    }

    /**
     * Will NOT Force load of Ontologies
     */
    public int getNumberOfLoadedOntologies() {
        if (ontologies == null) {
            return 0;
        }
        return ontologies.size();
    }


//    public Entity getOntologyById(int entityId) {
//        Collection gvCollection = getOntologyFacade();
//        Entity[] gvArray = (Entity[]) gvCollection.toArray(new Entity[0]);
//        for (Entity aGvArray : gvArray) {
//            if (aGvArray.getId() == entityId) return aGvArray;
//        }
//        return null;  //none found
//    }
//
//    public Entity getOntologyContaining(Entity nodeInModel) {
//        Collection gvCollection = getOntologyFacade();
//        Entity[] gvArray = (Entity[]) gvCollection.toArray(new Entity[0]);
//        long genomeVersionID = nodeInModel.getId();
//        for (Entity aGvArray : gvArray) {
//            if ((aGvArray).getId() == genomeVersionID) return aGvArray;
//        }
//        return null;  //none found
//    }

    public Task getCurrentAnnotationSessionTask() {
        return annotationSesisonTask;
    }

    public void setCurrentAnnotationSesisonTask(Task annotationSesisonTask) {
        this.annotationSesisonTask = annotationSesisonTask;
    }

    public Entity getSelectedOntology() {
        return selectedOntology;
    }

    public void setSelectedOntology(Entity ontology) {
        if (selectedOntology == null || !selectedOntology.getId().equals(ontology.getId())) {
            modelAvailable = true;
            selectedOntology = ontology;
            notifyOntologySelected(ontology);
        }
    }

    public void notifyOntologySelected(Entity ontology) {
        for (ModelMgrObserver listener : modelMgrObservers) {
        	listener.ontologySelected(ontology.getId());
        }
    }
    
    public void notifyEntitySelected(Entity entity) {
        for (ModelMgrObserver listener : modelMgrObservers) {
        	listener.entitySelected(entity.getId());
        }
    }

    public void notifyAnnotationsChanged(Entity entity) {
        for (ModelMgrObserver listener : modelMgrObservers) {
        	listener.annotationsChanged(entity.getId());
        }
    }

//    public void unSelectOntology(Entity ontology) {
//        selectedOntology = null;
//        if (modelMgrObservers != null) {
//            Object[] listeners = modelMgrObservers.toArray();
//            for (Object listener : listeners) {
//                ((ModelMgrObserver) listener).ontologyUnselected(ontology);
//            }
//        }
//        if (null == selectedOntology) modelAvailable = false;
//    }

    public void deleteAnnotation(Long annotatedEntityId, String tag) {
        FacadeManager.getFacadeManager().getAnnotationFacade().deleteAnnotation(annotatedEntityId, tag);
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

    public List<EntityType> getEntityTypes() {
        return FacadeManager.getFacadeManager().getEntityFacade().getEntityTypes();
    }

    public Entity getEntityById(String entityId) throws Exception {
        return FacadeManager.getFacadeManager().getEntityFacade().getEntityById(entityId);
    }

    public List<Entity> getEntitiesByName(String entityName) {
        return FacadeManager.getFacadeManager().getEntityFacade().getEntitiesByName(entityName);
    }

    public List<EntityData> getParentEntityDatas(Long childEntityId) {
        return FacadeManager.getFacadeManager().getEntityFacade().getParentEntityDatas(childEntityId);
    }

    public List<Entity> getEntitiesByType(Long entityTypeId) {
        return FacadeManager.getFacadeManager().getEntityFacade().getEntitiesByType(entityTypeId);
    }

    public boolean deleteEntityById(Long entityId) {
        return FacadeManager.getFacadeManager().getEntityFacade().deleteEntityById(entityId);
    }

    public void deleteEntityTree(Long id) {
        try {
            FacadeManager.getFacadeManager().getEntityFacade().deleteEntityTree(id);
        }
        catch (Exception e) {
            handleException(e);
        }
    }

    public Entity createOntologyAnnotation(String sessionId, String targetEntityId, String keyEntityId, String keyString, String valueEntityId, String valueString, String tag) throws Exception {
        return FacadeManager.getFacadeManager().getOntologyFacade().createOntologyAnnotation(sessionId, targetEntityId, keyEntityId, keyString, valueEntityId, valueString, tag);
    }

    public Entity createOntologyRoot(String ontologyName) throws Exception {
        return FacadeManager.getFacadeManager().getOntologyFacade().createOntologyRoot(ontologyName);
    }

    public EntityData createOntologyTerm(Long id, String label, OntologyElementType type, Integer orderIndex) throws Exception {
        return FacadeManager.getFacadeManager().getOntologyFacade().createOntologyTerm(id, label, type, orderIndex);
    }

    public Entity getOntologyTree(Long rootEntityId) throws Exception {
        return FacadeManager.getFacadeManager().getOntologyFacade().getOntologyTree(rootEntityId);
    }

    public List<Entity> getCommonRootEntitiesByType(long entityTypeId) {
        return FacadeManager.getFacadeManager().getEntityFacade().getCommonRootEntitiesByType(entityTypeId);
    }

    public Entity getEntityTree(long entityId) throws Exception {
        return FacadeManager.getFacadeManager().getEntityFacade().getEntityTree(entityId);
    }

    public Entity getCachedEntityTree(Long entityId) throws Exception {
        return FacadeManager.getFacadeManager().getEntityFacade().getCachedEntityTree(entityId);
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

    public Entity publishOntology(Long ontologyEntityId, String rootName) throws Exception {
        return FacadeManager.getFacadeManager().getOntologyFacade().publishOntology(ontologyEntityId, rootName);
    }

    public void removeOntologyTerm(Long termEntityId) throws Exception {
        FacadeManager.getFacadeManager().getOntologyFacade().removeOntologyTerm(termEntityId);
    }

    public Entity cloneEntityTree(Long entityId, String rootName) throws Exception {
        return FacadeManager.getFacadeManager().getEntityFacade().cloneEntityTree(entityId, rootName);
    }

    public List<Entity> getAnnotationsForEntity(Long entityId) throws Exception {
        return FacadeManager.getFacadeManager().getAnnotationFacade().getAnnotationsForEntity(entityId);
    }

    public List<Entity> getAnnotationsForEntities(List<Long> entityIds) throws Exception {
        return FacadeManager.getFacadeManager().getAnnotationFacade().getAnnotationsForEntities(entityIds);
    }

    public void removeAllOntologyAnnotationsForSession(Long annotationSessionId) throws Exception {
        FacadeManager.getFacadeManager().getAnnotationFacade().removeAllOntologyAnnotationsForSession(annotationSessionId);
    }

    public List<Entity> getEntitiesForAnnotationSession(Long annotationSessionId) throws Exception {
        return FacadeManager.getFacadeManager().getAnnotationFacade().getEntitiesForAnnotationSession(annotationSessionId);
    }

    public List<Entity> getCategoriesForAnnotationSession(Long annotationSessionId) throws Exception {
        return FacadeManager.getFacadeManager().getAnnotationFacade().getCategoriesForAnnotationSession(annotationSessionId);
    }

    public List<Entity> getAnnotationsForSession(Long annotationSessionId) throws Exception {
        return FacadeManager.getFacadeManager().getAnnotationFacade().getAnnotationsForSession(annotationSessionId);
    }

    public EntityData saveOrUpdateEntityData(EntityData newEntityData) throws Exception {
        return FacadeManager.getFacadeManager().getEntityFacade().saveEntityDataForEntity(newEntityData);
    }

    public Task saveOrUpdateTask(Task task) throws Exception {
        return FacadeManager.getFacadeManager().getComputeFacade().saveOrUpdateTask(task);
    }

    public void deleteTaskById(Long taskId) throws Exception {
        FacadeManager.getFacadeManager().getComputeFacade().deleteTaskById(taskId);
    }

    public List<Task> getUserTasksByType(String taskName) throws Exception {
        return FacadeManager.getFacadeManager().getComputeFacade().getUserTasksByType(taskName);
    }

    public User getUser() throws Exception {
        return FacadeManager.getFacadeManager().getComputeFacade().getUser();
    }

    public User saveOrUpdateUser(User user) throws Exception {
        return FacadeManager.getFacadeManager().getComputeFacade().saveOrUpdateUser(user);
    }

    public void removePreferenceCategory(String category) throws Exception {
        FacadeManager.getFacadeManager().getComputeFacade().removePreferenceCategory(category);
    }

    //  private void workSpaceWasCreated(GenomeVersion genomeVersion) {
//    Set genomeVersions=getGenomeVersions();
//    GenomeVersion gv;
//      for (Object genomeVersion1 : genomeVersions) {
//          gv = (GenomeVersion) genomeVersion1;
//          if (!genomeVersion.equals(gv)) gv.makeReadOnly();
//      }
//    FacadeManager.setGenomeVersionWithWorkSpaceId(genomeVersion.getID());
//    if (modelMgrObservers!=null) {
//       Object[] listeners=modelMgrObservers.toArray();
//        for (Object listener : listeners) {
//            ((ModelMgrObserver) listener).workSpaceCreated(genomeVersion);
//        }
//    }
//  }
//
//  private void workSpaceWasRemoved(GenomeVersion genomeVersion,Workspace workspace) {
//    FacadeManager.setGenomeVersionWithWorkSpaceId(0);
//    if (modelMgrObservers!=null) {
//       Object[] listeners=modelMgrObservers.toArray();
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
            ontologyLookupNeeded = true;
        }

        public void protocolRemovedFromInUseList(String protocol) {

        }
    }


}


