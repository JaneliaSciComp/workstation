package org.janelia.jacs2.persistence;

import org.janelia.jacs2.dao.ServiceInfoDao;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.service.ServiceInfo;
import org.janelia.jacs2.model.service.ServiceState;

import javax.inject.Inject;
import java.util.Set;

public class ServiceInfoPersistence extends AbstractDataPersistence<ServiceInfoDao, ServiceInfo, Long> {

    @Inject
    public ServiceInfoPersistence(ServiceInfoDao dao) {
        super(dao);
    }

    public PageResult<ServiceInfo> findServicesByState(Set<ServiceState> requestStates, PageRequest pageRequest) {
        return dao.findServicesByState(requestStates, pageRequest);
    }

}
