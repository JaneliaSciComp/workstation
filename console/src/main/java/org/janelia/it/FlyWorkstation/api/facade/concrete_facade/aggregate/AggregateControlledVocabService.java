/**
 * Title:        Your Product Name<p>
 * Description:  This is the main Browser in the System<p>
 * @author Peter Davies
 * @version
 */
package org.janelia.it.FlyWorkstation.api.facade.concrete_facade.aggregate;

import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.ControlledVocabService;
import org.janelia.it.FlyWorkstation.api.stub.data.NoData;
import org.janelia.it.jacs.shared.utils.ControlledVocabElement;

import java.util.ArrayList;
import java.util.List;

public class AggregateControlledVocabService extends AggregateFacadeBase implements ControlledVocabService {

  public ControlledVocabElement[] getControlledVocab
    (Long entityOID, int vocabIndex) throws NoData {
        Object[] aggregates=getAggregates();
        ControlledVocabElement[] tmpArray = null;
        List returnValues=new ArrayList(32);
      for (Object aggregate : aggregates) {
          tmpArray = ((ControlledVocabService) aggregate).
                  getControlledVocab(entityOID, vocabIndex);
          if (tmpArray != null) {
              // Add elements but preserve their order!
              // Cases of multiple aggregates contributing to a list have some
              // issues that are not dealt with very well here. There is no policy
              // defined for it.
              // Issue1: If more than one aggregate returns values for this vocab,
              //         then the order of the ControlledVocabElement objects in
              //         the list depends on the order of the aggregates from the
              //         getAggregates() call.
              // Issue2: If a subsequent aggregate has a ControlledVocabElement
              //         with the same key as one already added, that new one is
              //         discarded.
              for (ControlledVocabElement tmp : tmpArray) {
                  if (!returnValues.contains(tmp)) {
                      returnValues.add(tmp);
                  }
              }
          }
      }
         return (ControlledVocabElement[])returnValues.
           toArray(new ControlledVocabElement[returnValues.size()]);
  }

  protected String getMethodNameForAggregates(){
     return "getControlledVocabService";
  }

  protected Class[] getParameterTypesForAggregates(){
     return new Class[0];
  }

  protected  Object[] getParametersForAggregates(){
     return new Object[0];
  }
}