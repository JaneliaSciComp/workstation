package org.janelia.jacs2.sampleprocessing;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.janelia.jacs2.model.service.ServiceMetaData;
import org.janelia.jacs2.service.impl.ServiceDescriptor;
import org.janelia.jacs2.service.impl.ServiceProcessor;

import javax.inject.Inject;
import javax.inject.Named;

@Named("sampleSummary")
public class SampleSummaryServiceDescriptor implements ServiceDescriptor {
    private static String SERVICE_NAME = "sampleSummary";

    static class SampleSummaryArgs {
        @Parameter(names = "-sampleId", description = "Sample ID", required = true)
        Long sampleId;
        @Parameter(names = "-objective", description = "Sample objective", required = false)
        String sampleObjective;
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
        SampleSummaryArgs args = new SampleSummaryArgs();
        StringBuilder usageOutput = new StringBuilder();
        JCommander jc = new JCommander(args);
        jc.setProgramName(SERVICE_NAME);
        jc.usage(usageOutput);
        smd.setUsage(usageOutput.toString());
        return smd;
    }

    @Override
    public SampleSummaryProcessor createServiceProcessor() {
        return sampleSummaryProcessor;
    }
}
