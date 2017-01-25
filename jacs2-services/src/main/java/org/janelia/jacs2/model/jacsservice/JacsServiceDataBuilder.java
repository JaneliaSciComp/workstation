package org.janelia.jacs2.model.jacsservice;

public class JacsServiceDataBuilder {

    private final JacsServiceData serviceData = new JacsServiceData();

    public JacsServiceDataBuilder(JacsServiceData serviceContext) {
        if (serviceContext != null) {
            serviceData.setOwner(serviceContext.getOwner());
            serviceData.updateParentService(serviceContext);
            if (serviceContext.getProcessingLocation() != null) {
                serviceData.setProcessingLocation(serviceContext.getProcessingLocation());
            }
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

    public JacsServiceData build() {
        return serviceData;
    }
}
