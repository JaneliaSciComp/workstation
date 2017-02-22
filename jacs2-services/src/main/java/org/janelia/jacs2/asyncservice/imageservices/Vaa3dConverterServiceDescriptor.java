package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.Parameter;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceDescriptor;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;

import javax.inject.Inject;
import javax.inject.Named;

@Named("vaa3dConverter")
public class Vaa3dConverterServiceDescriptor implements ServiceDescriptor {
    static class Vaa3dConverterArgs extends ServiceArgs {
        @Parameter(names = "-convertCmd", description = "Convert command. Valid values are: []")
        String convertCmd = "-convert";
        @Parameter(names = "-input", description = "Input file", required = true)
        String inputFileName;
        @Parameter(names = "-output", description = "Output file", required = true)
        String outputFileName;
    }

    private final Vaa3dConverterProcessor vaa3dConverterProcessor;

    @Inject
    Vaa3dConverterServiceDescriptor(Vaa3dConverterProcessor vaa3dConverterProcessor) {
        this.vaa3dConverterProcessor = vaa3dConverterProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new Vaa3dConverterArgs());
    }

    @Override
    public Vaa3dConverterProcessor createServiceProcessor() {
        return vaa3dConverterProcessor;
    }

}
