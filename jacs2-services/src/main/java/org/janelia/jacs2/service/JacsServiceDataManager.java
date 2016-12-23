package org.janelia.jacs2.service;

import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.service.JacsServiceData;

import java.util.Date;

public interface JacsServiceDataManager {
    JacsServiceData retrieveServiceById(Long instanceId);
    PageResult<JacsServiceData> searchServices(JacsServiceData ref, Date from, Date to, PageRequest pageRequest);
    JacsServiceData submitServiceAsync(JacsServiceData serviceArgs);
    void setProcessingSlotsCount(int nProcessingSlots);
    ServerStats getServerStats();
}
