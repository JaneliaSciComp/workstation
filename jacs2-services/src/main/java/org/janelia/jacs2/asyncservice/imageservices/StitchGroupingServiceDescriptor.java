package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.Parameter;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceDescriptor;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;

import javax.inject.Inject;
import javax.inject.Named;

@Named("stitchGrouping")
public class StitchGroupingServiceDescriptor implements ServiceDescriptor {
    static class StitchGroupingArgs extends ServiceArgs {
        @Parameter(names = "-referenceChannelIndex", description = "Reference channel index", required = true)
        int referenceChannelIndex;
        @Parameter(names = "-inputDir", description = "Input directory path", required = true)
        String inputDir;
        @Parameter(names = "-resultDir", description = "Result directory", required = true)
        String resultDir;
    }

    private final StitchGroupingProcessor stitchGroupingProcessor;

    @Inject
    StitchGroupingServiceDescriptor(StitchGroupingProcessor stitchGroupingProcessor) {
        this.stitchGroupingProcessor = stitchGroupingProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new StitchGroupingArgs());
    }

    @Override
    public StitchGroupingProcessor createServiceProcessor() {
        return stitchGroupingProcessor;
    }
}
