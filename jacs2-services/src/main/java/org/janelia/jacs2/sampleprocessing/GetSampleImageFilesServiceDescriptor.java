package org.janelia.jacs2.sampleprocessing;

import org.janelia.jacs2.model.service.ServiceMetaData;
import org.janelia.jacs2.service.impl.ServiceDescriptor;

import javax.inject.Inject;
import javax.inject.Named;

@Named("getSampleImageFiles")
public class GetSampleImageFilesServiceDescriptor implements ServiceDescriptor {
    private static String SERVICE_NAME = "getSampleImageFiles";

    private final GetSampleImageFilesServiceProcessor sampleImageFilesProcessor;

    @Inject
    GetSampleImageFilesServiceDescriptor(GetSampleImageFilesServiceProcessor sampleImageFilesProcessor) {
        this.sampleImageFilesProcessor = sampleImageFilesProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        ServiceMetaData smd = new ServiceMetaData();
        smd.setServiceName(SERVICE_NAME);
        smd.setUsage(SampleServiceArgs.usage(SERVICE_NAME, new SampleServiceArgs()));
        return smd;
    }

    @Override
    public GetSampleImageFilesServiceProcessor createServiceProcessor() {
        return sampleImageFilesProcessor;
    }

}
