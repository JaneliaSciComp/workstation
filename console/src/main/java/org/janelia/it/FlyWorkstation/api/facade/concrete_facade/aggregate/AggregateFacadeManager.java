package org.janelia.it.FlyWorkstation.api.facade.concrete_facade.aggregate;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/22/11
 * Time: 4:58 PM
 */

import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.ControlledVocabService;
import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.OntologyLoader;
import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.fundtype.EntityLoader;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManagerBase;
import org.janelia.it.jacs.model.entity.EntityType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** This class provides the concrete implementation of FacadeManagerBase for the Aggregate Protocol */
public class AggregateFacadeManager extends FacadeManagerBase {

   private AggregateOntologyFacade ontologyFacade;
   private AggregateControlledVocabService vocabService;

   public AggregateFacadeManager() {
   }

   public String getDataSourceSelectorClass() {
      return "";
   }

   public Object[] getOpenDataSources() {
      List inUseProtocols = FacadeManager.getInUseProtocolStrings();
      String[] inUseProtocolArray = new String[inUseProtocols.size()];
      inUseProtocols.toArray(inUseProtocolArray);
      List dataSources = new ArrayList();
      Object[] tmpArray;
       for (String anInUseProtocolArray : inUseProtocolArray) {
           tmpArray = FacadeManager.getFacadeManager(anInUseProtocolArray).getOpenDataSources();
           dataSources.addAll(Arrays.asList(tmpArray));
       }
      return dataSources.toArray();
   }

   public String getServerName() {
      return "";
   }

   public EntityLoader getFacade(EntityType featureType) throws Exception {
      //Facades listed explicitly have some methods and therefore need to be coded
      //All others simply inherit Feature Facade and therefore can use the comman Interface
//      switch (featureType.getId().intValue()) {
//         case EntityTypeConstants.BlastN_Hit :
//         default :
//         {
       return new AggregateOntologyFacade();
//         }
//      }
   }

   public ControlledVocabService getControlledVocabService() throws Exception {
      if (vocabService == null)
         vocabService = new AggregateControlledVocabService();
      return vocabService;
   }

    @Override
    public OntologyLoader getOntologies() throws Exception {
        if (ontologyFacade == null)
           ontologyFacade = new AggregateOntologyFacade();
        return ontologyFacade;
    }

}
