package org.janelia.jacs2.asyncservice.common;

import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;

public interface ServiceProcessor<T> {
    ServiceMetaData getMetadata();
    JacsServiceData create(ServiceExecutionContext executionContext, ServiceArg ...args);
    ServiceComputation<JacsServiceData> invoke(ServiceExecutionContext executionContext, ServiceArg ...args);
    ServiceComputation<JacsServiceData> invokeAsync(ServiceExecutionContext executionContext, ServiceArg ...args);
    ServiceComputation<JacsServiceData> process(JacsServiceData jacsServiceData);
    T getResult(JacsServiceData jacsServiceData);
    void setResult(T result, JacsServiceData jacsServiceData);
}
