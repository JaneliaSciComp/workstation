package org.janelia.jacs2.asyncservice.common;

import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;

public interface ServiceProcessor<T> {
    ServiceMetaData getMetadata();
    ServiceComputation<T> invoke(ServiceExecutionContext executionContext, String ...args);
    ServiceComputation<JacsServiceData> invokeAsync(ServiceExecutionContext executionContext, String ...args);
    ServiceComputation<T> process(JacsServiceData jacsServiceData);
    T getResult(JacsServiceData jacsServiceData);
    void setResult(T result, JacsServiceData jacsServiceData);
}
