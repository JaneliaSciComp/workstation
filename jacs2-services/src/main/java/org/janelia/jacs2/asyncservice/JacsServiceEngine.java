package org.janelia.jacs2.asyncservice;

import org.janelia.jacs2.asyncservice.common.ServiceProcessor;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;

import java.util.List;

public interface JacsServiceEngine {
    void setProcessingSlotsCount(int nProcessingSlots);
    void setMaxWaitingSlots(int maxWaitingSlots);
    ServerStats getServerStats();
    ServiceProcessor<?> getServiceProcessor(JacsServiceData jacsServiceData);
    JacsServiceData submitSingleService(JacsServiceData serviceArgs);
    List<JacsServiceData> submitMultipleServices(List<JacsServiceData> listOfServices);
    boolean acquireSlot();
    void releaseSlot();
}
