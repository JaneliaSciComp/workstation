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
        @Parameter(names = "-imgFile", description = "The name of the image file", required = true)
        String imageFile;
        @Parameter(names = "-chanSpec", description = "Channel spec", required = true)
        String chanSpec;
        @Parameter(names = "-colorSpec", description = "Color spec", required = false)
        String colorSpec;
        @Parameter(names = "-laser", description = "Laser", required = false)
        Integer laser;
        @Parameter(names = "-gain", description = "Gain", required = false)
        Integer gain;
        @Parameter(names = "-resultsDir", description = "Results directory", required = false)
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
