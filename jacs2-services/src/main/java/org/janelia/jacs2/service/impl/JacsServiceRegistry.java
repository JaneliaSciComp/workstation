package org.janelia.jacs2.service.impl;

import org.janelia.jacs2.model.service.ServiceMetaData;
import org.janelia.jacs2.service.ServiceRegistry;
import org.slf4j.Logger;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class JacsServiceRegistry implements ServiceRegistry {

    @Inject
    private Logger logger;

    @Any @Inject
    private Instance<ServiceDescriptor> anyServiceSource;

    @Override
    public ServiceMetaData getServiceDescriptor(String serviceName) {
        ServiceDescriptor service = lookupService(serviceName);
        return service != null ? service.getMetadata() : null;
    }

    @Override
    public ServiceDescriptor lookupService(String serviceName) {
        for (ServiceDescriptor service : getAllServices(anyServiceSource)) {
            if (serviceName.equals(service.getMetadata().getServiceName())) {
                logger.info("Service found for {}", serviceName);
                return service;
            }
        }
        logger.error("NO Service found for {}", serviceName);
        return null;
    }

    @Inject
    private List<ServiceDescriptor> getAllServices(@Any Instance<ServiceDescriptor> services) {
        List<ServiceDescriptor> allServices = new ArrayList<>();
        for (ServiceDescriptor service : services) {
            allServices.add(service);
        }
        return allServices;
    }

}
