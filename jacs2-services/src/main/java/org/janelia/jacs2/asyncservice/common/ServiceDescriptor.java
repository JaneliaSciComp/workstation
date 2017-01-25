package org.janelia.jacs2.asyncservice.common;

import org.janelia.jacs2.model.jacsservice.ServiceMetaData;

public interface ServiceDescriptor {
    ServiceMetaData getMetadata();
    ServiceProcessor<?> createServiceProcessor();
}
