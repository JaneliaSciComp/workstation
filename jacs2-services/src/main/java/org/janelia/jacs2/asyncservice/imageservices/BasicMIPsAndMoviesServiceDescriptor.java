package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.Parameter;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceDescriptor;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;

import javax.inject.Inject;
import javax.inject.Named;

@Named("basicMIPsAndMovies")
public class BasicMIPsAndMoviesServiceDescriptor implements ServiceDescriptor {
    static class BasicMIPsAndMoviesArgs extends ServiceArgs {
        @Parameter(names = "-imgFiles", description = "list of image files", required = true)
        String imageFile;
        @Parameter(names = "-chanSpec", description = "Channel spec", required = true)
        String chanSpec;
        @Parameter(names = "-defaultChanSpec", description = "Channel spec", required = false)
        String defaultChanSpec;
        @Parameter(names = "-colorSpec", description = "Color spec", required = false)
        String colorSpec;
        @Parameter(names = "-divSpec", description = "Div spec", required = false)
        String divSpec;
        @Parameter(names = "-laser", description = "Laser", required = false)
        Integer laser;
        @Parameter(names = "-gain", description = "Gain", required = false)
        Integer gain;
        @Parameter(names = "-area", description = "Area", required = false)
        String area;
        @Parameter(names = "-objective", description = "Objective", required = false)
        String objective;
        @Parameter(names = "-resultDir", description = "Results directory", required = false)
        public String resultsDir;
        @Parameter(names = "-options", description = "Options", required = false)
        String options = "mips:movies:legends:bcomp";
    }

    private final BasicMIPsAndMoviesProcessor basicMIPsAndMoviesProcessor;

    @Inject
    BasicMIPsAndMoviesServiceDescriptor(BasicMIPsAndMoviesProcessor basicMIPsAndMoviesProcessor) {
        this.basicMIPsAndMoviesProcessor = basicMIPsAndMoviesProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new BasicMIPsAndMoviesArgs());
    }

    @Override
    public BasicMIPsAndMoviesProcessor createServiceProcessor() {
        return basicMIPsAndMoviesProcessor;
    }

}
