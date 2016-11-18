package org.janelia.jacs2.service.impl;

import org.janelia.jacs2.model.service.ServiceInfo;

/**
 * This contains the service data with the corresponding computation.
 */
class QueuedService {
    private final ServiceInfo serviceInfo;
    private final ServiceComputation serviceComputation;

    QueuedService(ServiceInfo serviceInfo, ServiceComputation serviceComputation) {
        this.serviceInfo = serviceInfo;
        this.serviceComputation = serviceComputation;
    }

    ServiceInfo getServiceInfo() {
        return serviceInfo;
    }

    ServiceComputation getServiceComputation() {
        return serviceComputation;
    }
}
