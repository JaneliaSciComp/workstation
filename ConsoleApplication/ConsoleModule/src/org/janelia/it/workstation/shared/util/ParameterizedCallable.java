package org.janelia.it.workstation.shared.util;

import java.util.concurrent.Callable;

/**
 * A callback that takes a parameter.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class ParameterizedCallable<T> implements Callable<Void> {
    
    private T param;

    void setParam(T param) {
        this.param = param;
    }

    T getParam() {
        return param;
    }
    
    public abstract void call(T param) throws Exception;

    @Override
    public Void call() throws Exception {
        call(param);
        return null;
    }

}