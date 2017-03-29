package org.janelia.jacs2.asyncservice.common;

import org.janelia.jacs2.model.jacsservice.JacsServiceData;

public interface ServiceResultHandler<T> {
    boolean isResultReady(JacsServiceData jacsServiceData);
    T collectResult(JacsServiceData jacsServiceData);
    void updateServiceDataResult(JacsServiceData jacsServiceData, T result);
    T getServiceDataResult(JacsServiceData jacsServiceData);
}
