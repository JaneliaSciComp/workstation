package org.janelia.jacs2.dao;

import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.model.service.JacsServiceState;

import java.util.Date;
import java.util.List;
import java.util.Set;

public interface JacsServiceDataDao extends Dao<JacsServiceData, Number> {
    List<JacsServiceData> findChildServices(Number serviceId);
    /**
     * Returns the service hierarchy for the service identified by <code>serviceId</code>
     * @param serviceId service identifier
     * @return service hierarchy as a pre-order traversal of the tree.
     */
    List<JacsServiceData> findServiceHierarchy(Number serviceId);
    PageResult<JacsServiceData> findMatchingServices(JacsServiceData pattern, Date from, Date to, PageRequest pageRequest);
    PageResult<JacsServiceData> findServiceByState(Set<JacsServiceState> requestStates, PageRequest pageRequest);
}
