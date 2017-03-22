package org.janelia.jacs2.asyncservice.common;

import org.janelia.jacs2.model.jacsservice.JacsServiceData;

public interface ServiceInfo<T> {
    T getResult();
    JacsServiceData getServiceData();
}
