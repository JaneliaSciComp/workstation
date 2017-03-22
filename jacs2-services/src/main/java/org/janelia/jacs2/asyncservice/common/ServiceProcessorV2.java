package org.janelia.jacs2.asyncservice.common;

import org.janelia.jacs2.model.jacsservice.ServiceMetaData;

public interface ServiceProcessorV2<T> {
    ServiceMetaData getMetadata();
    ServiceInfo<T> process(ServiceExecutionContext executionContext, ServiceArg... args);
}
