package org.janelia.jacs2.asyncservice.common;

import org.janelia.jacs2.model.jacsservice.JacsServiceData;

public interface ServiceProcessor<T> {
    ServiceComputation<T> process(JacsServiceData jacsServiceData);
    T getResult(JacsServiceData jacsServiceData);
    void setResult(T result, JacsServiceData jacsServiceData);
}
