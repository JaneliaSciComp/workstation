package org.janelia.jacs2.sampleprocessing;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.janelia.jacs2.model.service.ServiceMetaData;
import org.janelia.jacs2.service.impl.ServiceDescriptor;

import javax.inject.Inject;
import javax.inject.Named;

@Named("getSampleLsmMetadata")
public class GetSampleLsmsMetadataServiceDescriptor implements ServiceDescriptor {
    private static String SERVICE_NAME = "getSampleLsmMetadata";

    private final GetSampleLsmsMetadataProcessor sampleLsmMetadataProcessor;

    @Inject
    GetSampleLsmsMetadataServiceDescriptor(GetSampleLsmsMetadataProcessor sampleLsmMetadataProcessor) {
        this.sampleLsmMetadataProcessor = sampleLsmMetadataProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        ServiceMetaData smd = new ServiceMetaData();
        smd.setServiceName(SERVICE_NAME);
        smd.setUsage(SampleServiceArgs.usage(SERVICE_NAME, new SampleServiceArgs()));
        return smd;
    }

    @Override
    public GetSampleLsmsMetadataProcessor createServiceProcessor() {
        return sampleLsmMetadataProcessor;
    }

}
