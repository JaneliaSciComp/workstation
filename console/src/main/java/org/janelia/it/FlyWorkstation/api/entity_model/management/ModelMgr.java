package org.janelia.it.FlyWorkstation.api.entity_model.management;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrObserver;
import org.janelia.it.FlyWorkstation.api.entity_model.fundtype.ActiveThreadModel;
import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.OntologyLoader;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.InUseProtocolListener;
import org.janelia.it.FlyWorkstation.api.facade.roles.ExceptionHandler;
import org.janelia.it.FlyWorkstation.api.stub.data.NoData;
import org.janelia.it.FlyWorkstation.gui.framework.api.EJBFactory;
import org.janelia.it.FlyWorkstation.shared.exception_handlers.PrintStackTraceHandler;
import org.janelia.it.FlyWorkstation.shared.util.ThreadQueue;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.tasks.Task;

import java.lang.reflect.Constructor;
import java.util.*;

public class ModelMgr {
  private static ModelMgr modelManager=new ModelMgr();
  private boolean readOnly;
  private List<ModelMgrObserver> modelMgrObservers;
  private boolean modelAvailable;
  private Set<Entity> ontologies=new HashSet<Entity>();
  private ThreadQueue threadQueue;
  private ThreadQueue notificationQueue;
  private ResourceBundle modelMgrResourceBundle;
  private EntityFactory entityFactory;
  private Entity selectedOntology;
  private Task annotationSesisonTask;
  private Class factoryClass;
  private boolean ontologyLookupNeeded =true;

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

  static public ModelMgr getModelMgr() {return modelManager;}

  public void addModelMgrObserver(ModelMgrObserver mml){
     if (modelMgrObservers==null) modelMgrObservers=new ArrayList<ModelMgrObserver>();
     modelMgrObservers.add(mml);
  }

  public void removeModelMgrObserver(ModelMgrObserver mml){
     if (modelMgrObservers==null) return;
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

  public void makeReadOnly(){
     readOnly=true;
  }

  public EntityFactory getEntityFactory() {
    if (entityFactory==null) {
      try {
       Constructor cons=factoryClass.getConstructor(new Class[]{Integer.class});
       entityFactory = (EntityFactory)cons.newInstance(new Object[]{new Integer(this.hashCode())});
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
    FacadeManager.registerFacade(protocol,facadeClass,displayName);
  }

  public ThreadQueue getLoaderThreadQueue() {
      if (threadQueue==null)
        if (isMultiThreaded()) threadQueue=new ThreadQueue(6,"LoaderGroup",Thread.MIN_PRIORITY,true);
        else threadQueue=new ThreadQueue(0,"LoaderGroup",Thread.NORM_PRIORITY,true);
      return threadQueue;
  }

  public ThreadQueue getNotificationQueue() {
      if (notificationQueue==null)
        if (isMultiThreaded())  notificationQueue=new ThreadQueue(1,"NotificationThreads",Thread.MIN_PRIORITY,false);
        else notificationQueue=new ThreadQueue(0,"NotificationThreads",Thread.NORM_PRIORITY,false);
      return notificationQueue;
  }


  public ActiveThreadModel getActiveThreadModel() {
     return ActiveThreadModel.getActiveThreadModel();
  }

  public void handleException (Throwable throwable) {
     if (throwable instanceof NoData) return;
     FacadeManager.handleException(throwable);
  }

  public void removeAllOntologies() {
     ontologies=null;
  }

  public Set<Entity> getOntologies() {
     if (ontologyLookupNeeded) {
       OntologyLoader locator;
       try{
         locator = FacadeManager.getFacadeManager().getOntologies();
       }
       catch (Exception ex) {
          handleException(ex);
          return new HashSet<Entity>(0);
       }
       Entity[] versions=locator.getOntologies();
       Set<Entity> localGenomeVersions=new HashSet<Entity>(versions.length);
         for (Entity ontology : versions) {
//             if (readOnly && !ontology.isReadOnly()) ontology.makeReadOnly();
             localGenomeVersions.add(ontology);
             if (modelMgrObservers != null) {
                 Object[] listeners = modelMgrObservers.toArray();
                 for (Object listener : listeners) {
                     ((ModelMgrObserver) listener).ontologyAdded(ontology);
                 }
             }
         }
       ontologies.addAll(localGenomeVersions);
       ontologyLookupNeeded =false;
     }
     return new HashSet<Entity>(ontologies);
  }

  /**
  * Will NOT Force load of Ontologies
  */
  public int getNumberOfLoadedOntologies() {
    if (ontologies==null) { return 0; }
    return ontologies.size();
  }


  public Entity getOntologyById(int entityId) {
      Collection gvCollection=getOntologies();
      Entity[] gvArray=(Entity[])gvCollection.toArray(new Entity[0]);
      for (Entity aGvArray : gvArray) {
          if (aGvArray.getId() == entityId) return aGvArray;
      }
      return null;  //none found
  }

  public Entity getOntologyContaining(Entity nodeInModel) {
      Collection gvCollection=getOntologies();
      Entity[] gvArray=(Entity[])gvCollection.toArray(new Entity[0]);
      long genomeVersionID=nodeInModel.getId();
      for (Entity aGvArray : gvArray) {
          if (((Entity) aGvArray).getId() == genomeVersionID) return aGvArray;
      }
      return null;  //none found
  }

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
     if (selectedOntology==null || !selectedOntology.equals(ontology)) {
       modelAvailable=true;
         selectedOntology=ontology;
         if (modelMgrObservers!=null) {
         Object[] listeners=modelMgrObservers.toArray();
           for (Object listener : listeners) {
               ((ModelMgrObserver) listener).ontologySelected(ontology);
           }
       }
     }
  }

  public void unSelectOntology(Entity ontology) {
      selectedOntology = null;
       if (modelMgrObservers!=null) {
         Object[] listeners=modelMgrObservers.toArray();
           for (Object listener : listeners) {
               ((ModelMgrObserver) listener).ontologyUnselected(ontology);
           }
     }
    if (null==selectedOntology) modelAvailable=false;
  }

  public void deleteAnnotation(String userlogin, Long annotatedEntityId, String tag) {
      EJBFactory.getRemoteAnnotationBean().deleteAnnotation(userlogin, annotatedEntityId.toString(), tag);
  }

  public void prepareForSystemExit() {
     FacadeManager.getFacadeManager().prepareForSystemExit();
  }


  public boolean modelsDoUniquenessChecking() {
     String uc = modelMgrResourceBundle.getString("UniquenessCheckingOfEntities");
     if (uc!=null && uc.equalsIgnoreCase("TRUE")) return true;
     else return false;
  }

 public boolean isModelAvailable() {
    return modelAvailable;
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
     public void protocolAddedToInUseList(String protocol){
       ontologyLookupNeeded =true;
     }
     public void protocolRemovedFromInUseList(String protocol){

     }
  }


}


