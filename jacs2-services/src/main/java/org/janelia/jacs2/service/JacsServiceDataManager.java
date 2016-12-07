package org.janelia.jacs2.service;

import org.janelia.jacs2.model.service.JacsServiceData;

import java.util.Optional;

public interface JacsServiceDataManager {
    JacsServiceData retrieveServiceData(Long instanceId);
    JacsServiceData submitServiceAsync(JacsServiceData serviceArgs, Optional<JacsServiceData> parentService);
    void setProcessingSlotsCount(int nProcessingSlots);
    ServerStats getServerStats();
}
