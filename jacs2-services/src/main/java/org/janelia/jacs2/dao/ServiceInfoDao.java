package org.janelia.jacs2.dao;

import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.service.ServiceInfo;
import org.janelia.jacs2.model.service.ServiceState;

import java.util.List;
import java.util.Set;

/**
 * Created by goinac on 11/8/16.
 */
public interface ServiceInfoDao extends Dao<ServiceInfo, Long> {
    PageResult<ServiceInfo> findServicesByState(Set<ServiceState> requestStates, PageRequest pageRequest);
    List<ServiceInfo> findChildServices(Long serviceId);
    List<ServiceInfo> findServiceHierarchy(Long serviceId);
}
