package org.janelia.jacs2.service;

import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.service.JacsServiceData;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface JacsServiceDataManager {
    JacsServiceData retrieveServiceById(Long instanceId);
    PageResult<JacsServiceData> searchServices(JacsServiceData ref, Date from, Date to, PageRequest pageRequest);
    JacsServiceData submitServiceAsync(JacsServiceData serviceArgs, Optional<JacsServiceData> parentService);
    void setProcessingSlotsCount(int nProcessingSlots);
    ServerStats getServerStats();
}
