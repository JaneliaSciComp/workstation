package org.janelia.jacs2.asyncservice.common;

import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;

public interface ServiceProcessor<T> {
    ServiceMetaData getMetadata();
    ServiceComputation<T> process(JacsServiceData jacsServiceData);
    ServiceComputation<T> process(ServiceExecutionContext executionContext, ServiceArg ...args);
}
