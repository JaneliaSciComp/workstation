package org.janelia.jacs2.asyncservice.sampleprocessing;

import com.beust.jcommander.Parameter;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.janelia.jacs2.asyncservice.common.ServiceDescriptor;

import javax.inject.Inject;
import javax.inject.Named;

@Named("getSampleMIPsAndMovies")
public class GetSampleMIPsAndMoviesServiceDescriptor implements ServiceDescriptor {
    static class SampleMIPsAndMoviesArgs extends SampleServiceArgs {
        @Parameter(names = "-options", description = "Options", required = false)
        String options = "mips:movies:legends:bcomp";
        @Parameter(names = "-mipsSubDir", description = "MIPs and movies directory relative to sampleData directory", required = false)
        public String mipsSubDir = "mips";
    }

    private final GetSampleMIPsAndMoviesProcessor sampleMIPsAndMoviesProcessor;

    @Inject
    GetSampleMIPsAndMoviesServiceDescriptor(GetSampleMIPsAndMoviesProcessor sampleMIPsAndMoviesProcessor) {
        this.sampleMIPsAndMoviesProcessor = sampleMIPsAndMoviesProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new SampleMIPsAndMoviesArgs());
    }

    @Override
    public GetSampleMIPsAndMoviesProcessor createServiceProcessor() {
        return sampleMIPsAndMoviesProcessor;
    }

}
