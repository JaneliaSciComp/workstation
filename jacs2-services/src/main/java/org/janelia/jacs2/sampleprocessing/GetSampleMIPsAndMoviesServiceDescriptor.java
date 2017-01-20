package org.janelia.jacs2.sampleprocessing;

import com.beust.jcommander.Parameter;
import org.janelia.jacs2.model.service.ServiceMetaData;
import org.janelia.jacs2.service.impl.ServiceDescriptor;

import javax.inject.Inject;
import javax.inject.Named;

@Named("getSampleMIPsAndMovies")
public class GetSampleMIPsAndMoviesServiceDescriptor implements ServiceDescriptor {
    private static String SERVICE_NAME = "getSampleMIPsAndMovies";

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
        ServiceMetaData smd = new ServiceMetaData();
        smd.setServiceName(SERVICE_NAME);
        smd.setUsage(SampleMIPsAndMoviesArgs.usage(SERVICE_NAME, new SampleMIPsAndMoviesArgs()));
        return smd;
    }

    @Override
    public GetSampleMIPsAndMoviesProcessor createServiceProcessor() {
        return sampleMIPsAndMoviesProcessor;
    }

}
