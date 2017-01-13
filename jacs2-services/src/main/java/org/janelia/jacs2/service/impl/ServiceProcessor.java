package org.janelia.jacs2.service.impl;

import org.janelia.jacs2.model.service.JacsServiceData;

public interface ServiceProcessor<T> {
    ServiceComputation<T> process(JacsServiceData jacsServiceData);
    T getResult(JacsServiceData jacsServiceData);
    void setResult(T result, JacsServiceData jacsServiceData);
}
