package org.janelia.it.FlyWorkstation.api.facade.concrete_facade.aggregate;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/22/11
 * Time: 4:57 PM
 */

import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManagerBase;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.InUseProtocolListener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public abstract class AggregateFacadeBase implements InUseProtocolListener {
    private Method methodForAggregates;
    private List aggregateList = new ArrayList();

    public AggregateFacadeBase() {
        getAggregateReturningMethod();
    }

    public AggregateFacadeBase(boolean delayMethodRetrival) {
        if (!delayMethodRetrival) getAggregateReturningMethod();
    }

    protected void getAggregateReturningMethod() {
        try {
//        System.out.println("Constructing new AggFacBase");
            methodForAggregates = findMethodToGetAggregates(getMethodNameForAggregates(), getParameterTypesForAggregates());
//           System.out.println("Got the method: "+methodForAggregates.getName());
        }
        catch (NoSuchMethodException nosEx) {
            FacadeManager.handleException(nosEx);
        }
        FacadeManager.addInUseProtocolListener(this);
    }

    protected final int numAggregates() {
        return aggregateList.size();
    }

    protected final Object[] getAggregates() {
        return aggregateList.toArray();
    }

    protected abstract String getMethodNameForAggregates();

    protected abstract Class[] getParameterTypesForAggregates();

    protected abstract Object[] getParametersForAggregates();

    public void protocolAddedToInUseList(String protocol) {
//       System.out.println(this.getClass()+"Heard about: "+protocol);
        if (protocol.equals(FacadeManager.getAggregateProtocolString())) {
            return;
        }
        Object aggregate = null;
        try {
            aggregate = findMyAggregateForProtocol(methodForAggregates, getParametersForAggregates(), protocol);
//           System.out.println("Found Aggregate: "+aggregate);
        }
        catch (Exception ex) {
            FacadeManager.handleException(ex);
        }
        if (aggregate != null) aggregateList.add(aggregate);
    }


    public void protocolRemovedFromInUseList(String protocol) {
        // System.out.println(this.getClass()+"Heard about: "+protocol);
        Object aggregate = null;
        try {
            aggregate = findMyAggregateForProtocol(methodForAggregates, getParametersForAggregates(), protocol);
        }
        catch (Exception ex) {
            FacadeManager.handleException(ex);
        }
        if (aggregate != null) aggregateList.remove(aggregate);
    }

    private final Object findMyAggregateForProtocol(Method findMethod, Object[] parameters, String protocol) throws InvocationTargetException, IllegalAccessException {
//        System.out.println(this.getClass()+"Method: "+findMethod.getName()+" Prot: "+protocol);
        Object tmpObject = findMethod.invoke(FacadeManager.getFacadeManager(protocol), parameters);
        return tmpObject;
    }


    private final Method findMethodToGetAggregates(String methodName, Class[] parameterTypes) throws NoSuchMethodException {
        return FacadeManagerBase.class.getDeclaredMethod(methodName, parameterTypes);
    }

}
