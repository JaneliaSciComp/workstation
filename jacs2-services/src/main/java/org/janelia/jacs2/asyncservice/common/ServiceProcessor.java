package org.janelia.jacs2.asyncservice.common;

import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;

public interface ServiceProcessor<T> {
    ServiceMetaData getMetadata();
    JacsServiceData createServiceData(ServiceExecutionContext executionContext, ServiceArg... args);
    ServiceComputation<T> process(JacsServiceData jacsServiceData);
}
