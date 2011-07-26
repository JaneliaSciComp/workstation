package org.janelia.it.FlyWorkstation.api.facade.concrete_facade.aggregate;

import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.OntologyLoader;
import org.janelia.it.FlyWorkstation.api.stub.data.NoData;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/22/11
 * Time: 4:50 PM
 */
public class AggregateOntologyFacade extends AggregateFacadeBase implements OntologyLoader {
    Entity ontology;

    protected String getMethodNameForAggregates() {
       return ("getOntology");
    }

    protected Class[] getParameterTypesForAggregates() {
       return (new Class[0]);
    }

    protected  Object[] getParametersForAggregates() {
       return (new Object[0]);
    }

    @Override
    public Entity[] getOntologies() {
        Object[] aggregates=getAggregates();
        ArrayList rtnList = new ArrayList(aggregates.length);
        Entity[] tmpArray;
        int finalSize = 0;
        for (Object aggregate : aggregates) {
            tmpArray = ((OntologyLoader) aggregate).getOntologies();
            if (tmpArray != null) {
                rtnList.add(tmpArray);
                finalSize += tmpArray.length;
                tmpArray = null;
            }
        }
      //  if (finalSize==0) throw new NoData(); //if all facades return NoData, throw it
        tmpArray = new Entity[finalSize];
        int offset = 0;
        rtnList.trimToSize();
        for (Object aRtnList : rtnList) {
            System.arraycopy(aRtnList, 0, tmpArray, offset, ((EntityData[]) aRtnList).length);
            offset += ((EntityData[]) aRtnList).length;
        }
        return tmpArray;
    }

    @Override
    public EntityData[] getData(Long entityId) {
        return new EntityData[0];
    }

    public EntityData[] getProperties(Long genomicOID, EntityType dynamicType, boolean deepLoad) {
        Object[] aggregates=getAggregates();
        ArrayList rtnList = new ArrayList(aggregates.length);
        EntityData[] tmpArray;
        int finalSize = 0;
        for (Object aggregate : aggregates) {

            tmpArray = ((OntologyLoader) aggregate).getProperties(genomicOID, dynamicType, deepLoad);
            if (tmpArray != null) {
                rtnList.add(tmpArray);
                finalSize += tmpArray.length;
                tmpArray = null;
            }
        }
      //  if (finalSize==0) throw new NoData(); //if all facades return NoData, throw it
        tmpArray = new EntityData[finalSize];
        int offset = 0;
        rtnList.trimToSize();
        for (Object aRtnList : rtnList) {
            System.arraycopy(aRtnList, 0, tmpArray, offset, ((EntityData[]) aRtnList).length);
            offset += ((EntityData[]) aRtnList).length;
        }
        return tmpArray;
    }

    public EntityData[] expandProperty(Long genomicOID, String propertyName, EntityType dynamicType, boolean deepLoad) throws NoData {
        Object[] aggregates=getAggregates();
        ArrayList rtnList = new ArrayList(aggregates.length);
        EntityData[] tmpArray = null;
        int finalSize = 0;
        for (Object aggregate : aggregates) {
            try {
                tmpArray = ((OntologyLoader) aggregate).expandProperty(genomicOID, propertyName, dynamicType, deepLoad);
            }
            catch (NoData ndEx) {
                tmpArray = null;
                //do nothing here as any 1 facade might throw a NoData
            }
            if (tmpArray != null) {
                rtnList.add(tmpArray);
                finalSize += tmpArray.length;
            }
        }
        if (finalSize==0) throw new NoData(); //if all facades return NoData, throw it
        tmpArray = new EntityData[finalSize];
        int offset = 0;
        rtnList.trimToSize();
        for (Object aRtnList : rtnList) {
            System.arraycopy(aRtnList, 0, tmpArray, offset, ((EntityData[]) aRtnList).length);
            offset += ((EntityData[]) aRtnList).length;
        }
        return tmpArray;
    }

}
