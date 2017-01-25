package org.janelia.jacs2.asyncservice.sampleprocessing;

import com.beust.jcommander.Parameter;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.janelia.jacs2.asyncservice.common.ServiceDescriptor;

import javax.inject.Inject;
import javax.inject.Named;

@Named("sampleSummary")
public class SampleSummaryServiceDescriptor implements ServiceDescriptor {
    static class SampleSummaryArgs extends SampleServiceArgs {
        @Parameter(names = "-channelDyeSpec", description = "Channel dye spec", required = false)
        String channelDyeSpec;
    }

    private final SampleSummaryProcessor sampleSummaryProcessor;

    @Inject
    SampleSummaryServiceDescriptor(SampleSummaryProcessor sampleSummaryProcessor) {
        this.sampleSummaryProcessor = sampleSummaryProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        String serviceName = this.getClass().getAnnotation(Named.class).value();
        ServiceMetaData smd = new ServiceMetaData();
        smd.setServiceName(serviceName);
        smd.setUsage(SampleSummaryArgs.usage(serviceName, new SampleSummaryArgs()));
        return smd;
    }

    @Override
    public SampleSummaryProcessor createServiceProcessor() {
        return sampleSummaryProcessor;
    }
}
