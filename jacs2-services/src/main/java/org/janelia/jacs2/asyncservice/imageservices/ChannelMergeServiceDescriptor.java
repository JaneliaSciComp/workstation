package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.Parameter;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceDescriptor;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;

import javax.inject.Inject;
import javax.inject.Named;

@Named("mergeChannels")
public class ChannelMergeServiceDescriptor implements ServiceDescriptor {
    static class ChannelMergeArgs extends ServiceArgs {
        @Parameter(names = "-chInput1", description = "File containing the first set of channels", required = true)
        String chInput1;
        @Parameter(names = "-chInput2", description = "File containing the second set of channels", required = true)
        String chInput2;
        @Parameter(names = "-multiscanVersion", description = "Multiscan blend version", required = false)
        String multiscanBlendVersion;
        @Parameter(names = "-resultDir", description = "Result directory", required = true)
        String resultDir;
    }

    private final ChannelMergeProcessor channelMergeProcessor;

    @Inject
    ChannelMergeServiceDescriptor(ChannelMergeProcessor channelMergeProcessor) {
        this.channelMergeProcessor = channelMergeProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new ChannelMergeArgs());
    }

    @Override
    public ChannelMergeProcessor createServiceProcessor() {
        return channelMergeProcessor;
    }
}
