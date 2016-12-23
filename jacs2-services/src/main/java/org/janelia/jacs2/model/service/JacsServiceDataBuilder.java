package org.janelia.jacs2.model.service;

public class JacsServiceDataBuilder {

    private final JacsServiceData serviceData = new JacsServiceData();

    public JacsServiceDataBuilder(JacsServiceData serviceContext) {
        if (serviceContext != null) {
            serviceData.setOwner(serviceContext.getOwner());
            serviceData.updateParentService(serviceContext);
        }
    }

    public JacsServiceDataBuilder addArg(String... args) {
        for (String arg : args) {
            serviceData.addArg(arg);
        }
        return this;
    }

    public JacsServiceDataBuilder setName(String name) {
        serviceData.setName(name);
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
