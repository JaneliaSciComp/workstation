package org.janelia.jacs2.asyncservice.common.resulthandlers;

import org.janelia.jacs2.asyncservice.common.ServiceResultHandler;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;

public class EmptyServiceResultHandler<T> implements ServiceResultHandler<T> {
    @Override
    public boolean isResultReady(JacsServiceData jacsServiceData) {
        return true;
    }

    @Override
    public T collectResult(JacsServiceData jacsServiceData) {
        return null;
    }

    @Override
    public void updateServiceDataResult(JacsServiceData jacsServiceData, T result) {
    }

    @Override
    public T getServiceDataResult(JacsServiceData jacsServiceData) {
        return null;
    }
}
