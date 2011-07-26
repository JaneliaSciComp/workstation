package org.janelia.it.FlyWorkstation.api.facade.concrete_facade.xml;

import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.ControlledVocabService;
import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.OntologyLoader;
import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.fundtype.EntityLoader;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManagerBase;
import org.janelia.it.jacs.model.entity.EntityType;

/**
 * Base class for all XML-file facade managers.  Returns all types of API facades
 * and manages their creation.
 */
public abstract class XmlFacadeManager extends FacadeManagerBase {

   private OntologyLoader ontologyLoader = null;
   private ControlledVocabService controlledVocabularyService = null;
   public abstract String getDataSourceSelectorClass();

   /** Tells what has been opened. */
   public abstract Object[] getOpenDataSources();

   /**
    * Make this default constructor to build a detector for GV entity
    * removals.
    */
   public XmlFacadeManager() {
   } // End constructor

   /**
    * Called when system is about to close down, or when facade manager is
    * to be released.  Sort of like finalize (in an ideal world ;-)
    */
   public void prepareForSystemExit() {
      super.prepareForSystemExit();
      ontologyLoader=null;
      controlledVocabularyService = null;
   } // End method: prepareForSystemExit

   /** Returns the Ontology facade. */
   public OntologyLoader getOntology() throws Exception {
//      if (ontologyLoader == null) {
//         XmlOntologyLoader xmlOntologyLoader = new XmlOntologyLoader();
//         ontologyLoader = xmlOntologyLoader;
//      } // Need to create it.
//      return ontologyLoader;
       return null;
   } // End method: getOntology */

   /**
    * Returns name of server to satisfy the facade manager requirement.
    */
   public String getServerName() {
      return "XML";
   } // End method: getServerName

   /**
    * This is a request-decoder method.  All requests for "api facades"
    * should come through this method.
    */
   public EntityLoader getFacade(EntityType featureType) throws Exception {

//      switch (featureType.value()) {
//         case EntityTypeConstants.BlastN_Hit :
//         case EntityTypeConstants.BlastX_Hit :
//         case EntityTypeConstants.tBlastN :
//         case EntityTypeConstants.tBlastX :
//            return (this.getBlastHitFacade());
//
//
//         default :
//            return (this.getFeatureFacade());
       return null;
//      }
   } // End method: getFacade

   /** Return new or cached controlled vocab service. */
   public ControlledVocabService getControlledVocabService() throws Exception {
//      if (controlledVocabularyService == null)
//         controlledVocabularyService = new XmlControlledVocabService();
//
//      return controlledVocabularyService;
       return null;
   } // End method: getControlledVocabService

} // End class: XmlFacadeManager
