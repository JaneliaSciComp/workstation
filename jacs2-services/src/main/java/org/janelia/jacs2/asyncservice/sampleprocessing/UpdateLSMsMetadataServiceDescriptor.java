package org.janelia.jacs2.asyncservice.sampleprocessing;

import org.janelia.jacs2.asyncservice.common.ServiceArgs;
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
        return ServiceArgs.getMetadata(this.getClass(), new SampleServiceArgs());
    }

    @Override
    public UpdateLSMsMetadataProcessor createServiceProcessor() {
        return updateLSMsMetadataProcessor;
    }
}
