package org.janelia.jacs2.sampleprocessing;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.janelia.jacs2.model.service.ServiceMetaData;
import org.janelia.jacs2.service.impl.ServiceDescriptor;

import javax.inject.Inject;
import javax.inject.Named;

@Named("sampleSummary")
public class SampleSummaryServiceDescriptor implements ServiceDescriptor {
    private static String SERVICE_NAME = "sampleSummary";

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
        ServiceMetaData smd = new ServiceMetaData();
        smd.setServiceName(SERVICE_NAME);
        smd.setUsage(SampleSummaryArgs.usage(SERVICE_NAME, new SampleSummaryArgs()));
        return smd;
    }

    @Override
    public SampleSummaryProcessor createServiceProcessor() {
        return sampleSummaryProcessor;
    }
}
