package org.janelia.jacs2.model.jacsservice;

import java.util.Map;

public class JacsServiceDataBuilder {

    private final JacsServiceData serviceContext;
    private final JacsServiceData serviceData = new JacsServiceData();

    public JacsServiceDataBuilder(JacsServiceData serviceContext) {
        this.serviceContext = serviceContext;
        if (serviceContext != null) {
            serviceData.setOwner(serviceContext.getOwner());
            serviceData.updateParentService(serviceContext);
            if (serviceContext.getProcessingLocation() != null) {
                serviceData.setProcessingLocation(serviceContext.getProcessingLocation());
            }
            serviceData.setWorkspace(serviceContext.getWorkspace());
        }
    }

    public JacsServiceDataBuilder addArg(String... args) {
        for (String arg : args) {
            serviceData.addArg(arg);
        }
        return this;
    }

    public JacsServiceDataBuilder clearArgs() {
        serviceData.clearArgs();
        return this;
    }

    public JacsServiceDataBuilder setName(String name) {
        serviceData.setName(name);
        return this;
    }

    public JacsServiceDataBuilder setProcessingLocation(ProcessingLocation processingLocation) {
        serviceData.setProcessingLocation(processingLocation);
        return this;
    }

    public JacsServiceDataBuilder setState(JacsServiceState state) {
        serviceData.setState(state);
        return this;
    }

    public JacsServiceDataBuilder addDependency(JacsServiceData serviceDependency) {
        serviceData.addServiceDependency(serviceDependency);
        return this;
    }

    public JacsServiceDataBuilder addDependencyId(Number serviceDependencyId) {
        serviceData.addServiceDependencyId(serviceDependencyId);
        return this;
    }

    public JacsServiceDataBuilder setWorkspace(String workspace) {
        serviceData.setWorkspace(workspace);
        return this;
    }

    public JacsServiceDataBuilder setDescription(String description) {
        serviceData.setDescription(description);
        return this;
    }

    public JacsServiceDataBuilder copyResourcesFrom(Map<String, String> resources) {
        serviceData.getResources().putAll(resources);
        return this;
    }

    public JacsServiceData build() {
        if (serviceContext != null) {
            serviceContext.addEvent(JacsServiceEventTypes.CREATE_CHILD_SERVICE, String.format("Create child service %s %s", serviceData.getName(), serviceData.getArgs()));
        }
        return serviceData;
    }
}
