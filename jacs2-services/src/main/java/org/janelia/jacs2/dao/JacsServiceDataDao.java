package org.janelia.jacs2.dao;

import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.model.service.JacsServiceState;

import java.util.List;
import java.util.Set;

public interface JacsServiceDataDao extends Dao<JacsServiceData, Long> {
    PageResult<JacsServiceData> findServiceByState(Set<JacsServiceState> requestStates, PageRequest pageRequest);
    List<JacsServiceData> findChildServices(Long serviceId);
    List<JacsServiceData> findServiceHierarchy(Long serviceId);
}
