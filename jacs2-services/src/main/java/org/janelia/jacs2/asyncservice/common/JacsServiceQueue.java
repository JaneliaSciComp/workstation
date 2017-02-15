package org.janelia.jacs2.asyncservice.common;

import org.janelia.jacs2.model.jacsservice.JacsServiceData;

import java.util.List;

public interface JacsServiceQueue {
    int getMaxReadyCapacity();
    void setMaxReadyCapacity(int maxReadyCapacity);
    JacsServiceData enqueueService(JacsServiceData jacsServiceData);
    JacsServiceData dequeService();
    void refreshServiceQueue();
    void completeService(JacsServiceData jacsServiceData);
    int getReadyServicesSize();
    int getPendingServicesSize();
    List<Number> getPendingServices();
}
