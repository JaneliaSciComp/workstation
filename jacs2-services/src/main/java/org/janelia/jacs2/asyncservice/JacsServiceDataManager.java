package org.janelia.jacs2.asyncservice;

import org.janelia.jacs2.model.DataInterval;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;

import java.util.Date;
import java.util.List;

public interface JacsServiceDataManager {
    JacsServiceData retrieveServiceById(Long instanceId);
    PageResult<JacsServiceData> searchServices(JacsServiceData ref, DataInterval<Date> creationInterval, PageRequest pageRequest);
    JacsServiceData createSingleService(JacsServiceData serviceArgs);
    List<JacsServiceData> createMultipleServices(List<JacsServiceData> listOfServices);
    JacsServiceData updateService(Long instanceId, JacsServiceData serviceData);
}
