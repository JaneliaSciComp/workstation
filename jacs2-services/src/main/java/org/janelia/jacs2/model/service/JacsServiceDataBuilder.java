package org.janelia.jacs2.model.service;

public class JacsServiceDataBuilder {

    private final JacsServiceData serviceContext;
    private final JacsServiceData serviceData = new JacsServiceData();

    public JacsServiceDataBuilder(JacsServiceData serviceContext) {
        this.serviceContext = serviceContext;
        if (serviceContext != null) {
            serviceData.setOwner(serviceContext.getOwner());
            serviceData.setPriority(serviceContext.priority() + 1);
            serviceData.updateParentService(serviceContext);
        }
    }

    public JacsServiceDataBuilder addArg(String... args) {
        for (String arg : args) {
            serviceData.addArg(arg);
        }
        return this;
    }

    public JacsServiceDataBuilder setOwner(String owner) {
        serviceData.setOwner(owner);
        return this;
    }

    public JacsServiceDataBuilder setPriority(int priority) {
        serviceData.setPriority(priority);
        return this;
    }

    public JacsServiceData build() {
        return serviceData;
    }
}
