package org.janelia.jacs2.asyncservice.sampleprocessing;

import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.janelia.jacs2.asyncservice.common.ServiceDescriptor;

import javax.inject.Inject;
import javax.inject.Named;

@Named("getSampleLsmMetadata")
public class GetSampleLsmsMetadataServiceDescriptor implements ServiceDescriptor {
    private final GetSampleLsmsMetadataProcessor sampleLsmMetadataProcessor;

    @Inject
    GetSampleLsmsMetadataServiceDescriptor(GetSampleLsmsMetadataProcessor sampleLsmMetadataProcessor) {
        this.sampleLsmMetadataProcessor = sampleLsmMetadataProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new SampleServiceArgs());
    }

    @Override
    public GetSampleLsmsMetadataProcessor createServiceProcessor() {
        return sampleLsmMetadataProcessor;
    }

}
