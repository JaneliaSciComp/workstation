package org.janelia.jacs2.dao;

import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.model.service.JacsServiceState;

import java.util.List;
import java.util.Set;

public interface JacsServiceDataDao extends Dao<JacsServiceData, Number> {
    PageResult<JacsServiceData> findServiceByState(Set<JacsServiceState> requestStates, PageRequest pageRequest);
    List<JacsServiceData> findChildServices(Number serviceId);
    List<JacsServiceData> findServiceHierarchy(Number serviceId);
}
