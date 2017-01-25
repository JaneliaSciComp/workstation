package org.janelia.jacs2.asyncservice.sampleprocessing;

import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.janelia.jacs2.asyncservice.common.ServiceDescriptor;

import javax.inject.Inject;
import javax.inject.Named;

@Named("updateLSMMetadata")
public class UpdateLSMsMetadataServiceDescriptor implements ServiceDescriptor {
    private final UpdateLSMsMetadataProcessor updateLSMsMetadataProcessor;

    @Inject
    UpdateLSMsMetadataServiceDescriptor(UpdateLSMsMetadataProcessor updateLSMsMetadataProcessor) {
        this.updateLSMsMetadataProcessor = updateLSMsMetadataProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        String serviceName = this.getClass().getAnnotation(Named.class).value();
        ServiceMetaData smd = new ServiceMetaData();
        smd.setServiceName(serviceName);
        smd.setUsage(SampleServiceArgs.usage(serviceName, new SampleServiceArgs()));
        return smd;
    }

    @Override
    public UpdateLSMsMetadataProcessor createServiceProcessor() {
        return updateLSMsMetadataProcessor;
    }
}
