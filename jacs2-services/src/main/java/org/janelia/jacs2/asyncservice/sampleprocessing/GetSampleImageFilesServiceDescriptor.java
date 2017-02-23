package org.janelia.jacs2.asyncservice.sampleprocessing;

import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.janelia.jacs2.asyncservice.common.ServiceDescriptor;

import javax.inject.Inject;
import javax.inject.Named;

@Named("getSampleImageFiles")
public class GetSampleImageFilesServiceDescriptor implements ServiceDescriptor {
    private final GetSampleImageFilesServiceProcessor sampleImageFilesProcessor;

    @Inject
    GetSampleImageFilesServiceDescriptor(GetSampleImageFilesServiceProcessor sampleImageFilesProcessor) {
        this.sampleImageFilesProcessor = sampleImageFilesProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new SampleServiceArgs());
    }

    @Override
    public GetSampleImageFilesServiceProcessor createServiceProcessor() {
        return sampleImageFilesProcessor;
    }

}
